package it.unitn.disi.newscasting;

import it.unitn.disi.epidemics.IApplicationConfigurator;
import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IEventStorage;
import it.unitn.disi.epidemics.IMessageVisibility;
import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.epidemics.IContentExchangeStrategy.ActivityStatus;
import it.unitn.disi.newscasting.internal.LoggingObserver;
import it.unitn.disi.newscasting.internal.ProfilePageMulticast;
import it.unitn.disi.newscasting.internal.SimpleEventStorage;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.forwarding.BitsetHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.BloomFilterHistoryFw;
import it.unitn.disi.newscasting.internal.forwarding.CachingHistoryFw;
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

import peersim.config.IResolver;
import peersim.core.Node;

public class HistoryForwardingTest extends PeerSimTest {
	
	private int mode;
	
	@Test
	public void bloomFilterIndirectDissemination() {
		mode = CustomConfigurator.BLOOMFILTERS;
		this.indirectDissemination();
	}
	
	@Test
	public void bloomFilterLearnsHistoryFromDuplicate() {
		mode = CustomConfigurator.BLOOMFILTERS;
		this.learnsHistoryFromDuplicate();
	}

	@Test
	public void bitSetIndirectDissemination() {
		mode = CustomConfigurator.BITSET;
		this.indirectDissemination();
	}
	
	@Test
	public void bitSetLearnsHistoryFromDuplicate() {
		mode = CustomConfigurator.BITSET;
		this.learnsHistoryFromDuplicate();
	}

	public void indirectDissemination(){

		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(4);
		
		final int CYCLES = 4;
		
		final int SOCIAL_NETWORK_ID = builder.assignLinkable(new long[][] {
				{0, 1, 2, 3},
				{0, 2},
				{0, 1, 3},
				{0, 2}
		});
				
		final int SELECTOR_PID = DeterministicSelector.assignSchedule(builder,
				SOCIAL_NETWORK_ID, new Long[][] {
			{null,	null,	null,	null},
			{2L, 	null,	null,	0L	},
			{null,	3L,		null,	0L	},
			{null,	null,	0L,		null},
		});
		
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		List <? extends Node> nodes = builder.getNodes(); 
		// Creates a root tweet which will be known by everyone.
		Node profileOwner = nodes.get(0);
		IMessageVisibility vis = new ProfilePageMulticast(SOCIAL_NETWORK_ID);
		Tweet root = new Tweet(profileOwner, 0, vis);
		
		final int SOCIAL_NEWSCASTING_ID = initSocialNewscasting(builder, SOCIAL_NETWORK_ID,
				SELECTOR_PID, log, nodes, root, mode);
		
		builder.done();

		// Posts a reply to our tweet.
		Node replier = nodes.get(1);
		ISocialNewscasting snsapp = (ISocialNewscasting) replier.getProtocol(SOCIAL_NEWSCASTING_ID);
		Tweet reply = snsapp.replyToPost(root);

		FakeCycleEngine engine = new FakeCycleEngine(nodes, 42);
		engine.run(CYCLES);
		
		for (Node node : nodes) {
			IProtocolSet protocols = (IProtocolSet) node.getProtocol(SOCIAL_NEWSCASTING_ID);
			IApplicationInterface app = (IApplicationInterface) protocols;
			IContentExchangeStrategy strategy = (IContentExchangeStrategy) protocols
					.getStrategy(CachingHistoryFw.class);
			
			// All instances should be quiescent.
			Assert.assertEquals("Node:" + node.getID(), IContentExchangeStrategy.ActivityStatus.QUIESCENT, strategy.status());
			
			// All instances should know the two tweets.
			IEventStorage storage = app.storage();
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
		
	public void learnsHistoryFromDuplicate() {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(4);
		
		final int CYCLES = 4;
		
		final int SOCIAL_NETWORK_ID = builder.assignLinkable(new long[][] {
				{1, 2, 3},
				{0, 2, 3},
				{0, 1, 3},
				{0, 1, 2}
		});
				
		final int SELECTOR_PID = DeterministicSelector.assignSchedule(builder,
				SOCIAL_NETWORK_ID, new Long[][] {
			{1L,	null,	3L,		2L	},
			{null, 	2L,		null,	null},
			{null,	null,	null,	null},
			{null,	null,	null,	null},
		});
		
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		List<? extends Node> nodes = builder.getNodes();

		final int SOCIAL_NEWSCASTING_ID = initSocialNewscasting(builder,
				SOCIAL_NETWORK_ID, SELECTOR_PID, log, nodes, null, mode);
		
		builder.done();
		
		Node root = nodes.get(0);
		ISocialNewscasting snsapp = (ISocialNewscasting) root.getProtocol(SOCIAL_NEWSCASTING_ID);
		Tweet post = snsapp.postToFriends();
		
		FakeCycleEngine engine = new FakeCycleEngine(nodes, 42);
		engine.run(CYCLES);
		
		assertQuiescence(true, SOCIAL_NEWSCASTING_ID, nodes.get(2));
		
		EventMatcher matcher = new EventMatcher(new EventCodec(Byte.class, NewscastEvents.values()));
		matcher.addEvent(NewscastEvents.TWEETED.magicNumber(), post.poster.getID(), post.sequenceNumber, 0L);
		matcher.addEvent(NewscastEvents.DELIVER_SINGLE_TWEET.magicNumber(), post.poster.getID(), 0L, 1L, post.sequenceNumber, 0L);
		matcher.addEvent(NewscastEvents.DELIVER_SINGLE_TWEET.magicNumber(), post.poster.getID(), 1L, 2L, post.sequenceNumber, 1L);
		matcher.addEvent(NewscastEvents.DELIVER_SINGLE_TWEET.magicNumber(), post.poster.getID(), 0L, 3L, post.sequenceNumber, 2L);
		matcher.addEvent(NewscastEvents.DUPLICATE_TWEET.magicNumber(), post.poster.getID(), 0L, 2L, post.sequenceNumber, 3L);
		matcher.match(new ByteArrayInputStream(log.toByteArray()));
	}
	
	@Test
	public void doesntSendBackToSender() {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(2);
		
		final int CYCLES = 4;
		
		final int SOCIAL_NETWORK_ID = builder.assignLinkable(new long[][] {
				{1},
				{0}
		});
				
		final int SELECTOR_PID = DeterministicSelector.assignSchedule(builder,
				SOCIAL_NETWORK_ID, new Long[][] {
			{1L,	null,	1L,		null},
			{null, 	0L,		null,	0L	}
		});

		ByteArrayOutputStream log = new ByteArrayOutputStream();
		List<? extends Node> nodes = builder.getNodes();

		final int SOCIAL_NEWSCASTING_ID = initSocialNewscasting(builder,
				SOCIAL_NETWORK_ID, SELECTOR_PID, log, nodes, null, CustomConfigurator.NOHIST);
		
		builder.done();
		
		Node root = nodes.get(0);
		ISocialNewscasting snsapp = (ISocialNewscasting) root.getProtocol(SOCIAL_NEWSCASTING_ID);
		Tweet post = snsapp.postToFriends();
		
		FakeCycleEngine engine = new FakeCycleEngine(nodes, 42);
		engine.run(CYCLES);

		EventMatcher matcher = new EventMatcher(new EventCodec(Byte.class, NewscastEvents.values()));
		matcher.addEvent(NewscastEvents.TWEETED.magicNumber(), post.poster.getID(), post.sequenceNumber, 0L);
		matcher.addEvent(NewscastEvents.DELIVER_SINGLE_TWEET.magicNumber(), post.poster.getID(), 0L, 1L, post.sequenceNumber, 0L);
		
		matcher.match(new ByteArrayInputStream(log.toByteArray()));
	}

	private int initSocialNewscasting(TestNetworkBuilder builder,
			final int SOCIAL_NETWORK_ID, final int SELECTOR_PID,
			ByteArrayOutputStream log, List<? extends Node> nodes, Tweet root, int mode) {
		int pid = -1;
		for (int i = 0; i < nodes.size(); i++) {
			SocialNewscastingService sns = new SocialNewscastingService(null,
					Math.max(SOCIAL_NETWORK_ID, SELECTOR_PID) + 1,
					SOCIAL_NETWORK_ID, new CustomConfigurator(
							new ProtocolReference<IPeerSelector>(SELECTOR_PID),
							mode, SOCIAL_NETWORK_ID));
			sns.initialize(nodes.get(i));

			LoggingObserver logger = new LoggingObserver(log, true);
			sns.addSubscriber(logger);

			IWritableEventStorage storage = new SimpleEventStorage();
			sns.setStorage(storage);
			if (root != null) {
				storage.add(root);
			}
			pid = builder.addProtocol(nodes.get(i), sns);
		}
		return pid;
	}

	private void assertQuiescence(boolean isTrue, int pid, Node...nodes) {
		for (Node node : nodes) {
			SocialNewscastingService sns = (SocialNewscastingService) node.getProtocol(pid);
			HistoryForwarding fw = sns.getStrategy(HistoryForwarding.class);
			Assert.assertEquals(isTrue, fw.status() == ActivityStatus.QUIESCENT);
		}
	}
	
	static class CustomConfigurator implements IApplicationConfigurator {
		
		public static final int NOHIST = 0;
		
		public static final int BLOOMFILTERS = 1;
		
		public static final int BITSET = 2;
		
		private IReference<IPeerSelector> fSelector;
		
		private int fMode;
		
		private int fSnId;
		
		public CustomConfigurator(IReference<IPeerSelector> selector, int mode, int snid) {
			fSelector = selector;
			fMode = mode;
		}

		@Override
		@SuppressWarnings("unchecked")
		public void configure(IProtocolSet app, IResolver resolver, String prefix) {
			HistoryForwarding fw;
			SocialNewscastingService sns = (SocialNewscastingService) app;
			switch(fMode) {
			
			case BLOOMFILTERS:
				fw = new BloomFilterHistoryFw(sns.pid(), fSnId, 1,
						50, 0.001);
				sns.addStrategy(new Class[] { BloomFilterHistoryFw.class,
						CachingHistoryFw.class, HistoryForwarding.class }, fw,
						fSelector, new FallThroughReference<ISelectionFilter>(
								fw));
				break;
			
			case NOHIST:
				fw = new HistoryForwarding(sns.pid(), fSnId, 1);
				sns.addStrategy(new Class[] { HistoryForwarding.class }, fw,
						fSelector, new FallThroughReference<ISelectionFilter>(
								fw));
				break;
			
			case BITSET:
				fw = new BitsetHistoryFw(sns.pid(), fSnId, 1, 50);
				sns.addStrategy(new Class[] { BitsetHistoryFw.class,
						CachingHistoryFw.class, HistoryForwarding.class }, fw,
						fSelector, new FallThroughReference<ISelectionFilter>(
								fw));
				break;
			
			default:
				throw new IllegalArgumentException();
			}
			sns.addSubscriber(fw);
		}
		
		public Object clone () {
			return null;
		}

	}
}
