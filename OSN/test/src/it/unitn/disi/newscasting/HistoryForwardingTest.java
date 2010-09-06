package it.unitn.disi.newscasting;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.SimpleEventStorage;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import it.unitn.disi.test.framework.EventMatcher;
import it.unitn.disi.test.framework.FakeCycleEngine;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import peersim.core.CommonState;
import peersim.core.Node;

public class HistoryForwardingTest extends PeerSimTest {

	@Test
	public void indirectDissemination(){

		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.mkNodeArray(4);
		
		final int CYCLES = 4;
		
		final int SOCIAL_NETWORK_ID = builder.assignLinkable(new long[][] {
				{0, 1, 2, 3},
				{0, 2},
				{0, 1, 3},
				{0, 2}
		});
				
		DeterministicSelector.assignSchedule(builder,
				SOCIAL_NETWORK_ID, new Long[][] {
			{null,	null,	null,	null},
			{2L, 	null,	null,	0L	},
			{null,	3L,		null,	0L	},
			{null,	null,	0L,		null},
		});
		
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		List <Node> nodes = builder.getNodes(); 

		// Creates a root tweet which will be known by everyone.
		Node profileOwner = nodes.get(0);
		Tweet root = new Tweet(profileOwner, 0);
		
		int pid = -1;
		for (int i = 0; i < nodes.size(); i++) {
			SocialNewscastingService sns = new SocialNewscastingService(null, 2,
					SOCIAL_NETWORK_ID, new CustomConfigurator(
							new ProtocolReference<IPeerSelector>(1)));
			
			LoggingObserver logger = new LoggingObserver(log, true);
			sns.addSubscriber(logger);

			IWritableEventStorage storage = new SimpleEventStorage();
			storage.add(root);
			sns.setStorage(storage);
			
			pid = builder.addProtocol(nodes.get(i), sns);
		}
		
		final int SOCIAL_NEWSCASTING_ID = pid;
		
		builder.replayAll();

		FakeCycleEngine engine = new FakeCycleEngine(nodes, 42);

		// Posts a reply to our tweet.
		Node replier = nodes.get(1);
		IApplicationInterface snsapp = (IApplicationInterface) replier.getProtocol(SOCIAL_NEWSCASTING_ID);
		CommonState.setNode(replier);
		Tweet reply = snsapp.replyToPost(root);
		
		engine.run(CYCLES);
		
		for (Node node : nodes) {
			ICoreInterface snscore = (ICoreInterface) node.getProtocol(SOCIAL_NEWSCASTING_ID);
			IContentExchangeStrategy strategy = (IContentExchangeStrategy) snscore
					.getStrategy(BloomFilterHistoryFw.class);
			
			// All instances should be quiescent.
			Assert.assertEquals("Node:" + node.getID(), IContentExchangeStrategy.ActivityStatus.QUIESCENT, strategy.status());
			
			// All instances should know the two tweets.
			IEventStorage storage = snscore.storage();
			Assert.assertEquals(2, storage.elements());
			Assert.assertTrue(storage.contains(root));
			Assert.assertTrue(storage.contains(reply));
		}
		
		// Now matches event by event.
		EventMatcher matcher = new EventMatcher(new EventCodec(Byte.class, NewscastEvents.values()));
		matcher.addEvent(NewscastEvents.TWEETED.magicNumber(), reply.poster.getID(), reply.sequenceNumber, 0L);
		matcher.addEvent(NewscastEvents.DELIVER_SINGLE_TWEET.magicNumber(), reply.poster.getID(), 1L, 2L, reply.sequenceNumber, 0L);
		matcher.addEvent(NewscastEvents.DELIVER_SINGLE_TWEET.magicNumber(), reply.poster.getID(), 2L, 3L, reply.sequenceNumber, 1L);
		matcher.addEvent(NewscastEvents.DELIVER_SINGLE_TWEET.magicNumber(), reply.poster.getID(), 3L, 0L, reply.sequenceNumber, 2L);
		matcher.addEvent(NewscastEvents.DUPLICATE_TWEET.magicNumber(), reply.poster.getID(), 1L, 0L, reply.sequenceNumber, 3L);
		matcher.addEvent(NewscastEvents.DUPLICATE_TWEET.magicNumber(), reply.poster.getID(), 2L, 0L, reply.sequenceNumber, 3L);
		
		matcher.match(new ByteArrayInputStream(log.toByteArray()));
	}
	
	class CustomConfigurator implements IApplicationConfigurator {
		
		private IReference<IPeerSelector> fSelector;
		
		public CustomConfigurator(IReference<IPeerSelector> selector) {
			fSelector = selector;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void configure(SocialNewscastingService app, String prefix, int protocolId,
				int socialNetworkId) {
			BloomFilterHistoryFw fw = new BloomFilterHistoryFw(protocolId, socialNetworkId, 1, 50, 0.001);
			app.addStrategy(new Class[]{ BloomFilterHistoryFw.class, HistoryForwarding.class }, 
					fw, fSelector, new FallThroughReference<ISelectionFilter>(fw), 1.0);
			app.addSubscriber(fw);
		}
		
		public Object clone () {
			return null;
		}

	}
}
