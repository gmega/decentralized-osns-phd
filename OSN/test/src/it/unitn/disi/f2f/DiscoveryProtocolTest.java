package it.unitn.disi.f2f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.NullConfigurator;
import it.unitn.disi.epidemics.MulticastService;
import it.unitn.disi.newscasting.DeterministicSelector;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.SimpleEventStorage;
import it.unitn.disi.newscasting.internal.demers.DemersRumorMonger;
import it.unitn.disi.test.framework.FakeCycleEngine;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TabularLogMatcher;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.streams.TeeOutputStream;

import org.junit.Test;

import peersim.core.Linkable;
import peersim.core.Node;

public class DiscoveryProtocolTest extends PeerSimTest {
	
	private static final double GIVEUP = 1.0;
	
	private static final Random fRand = new Random();
	
	@Test
	public void testMulticastCollect() throws IOException {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(8);
		
		int MEMBERSHIP_ID = builder.assignLinkable(new long[][] { 
				{ 1, 4, 7 },
				{ 0, 2, 3 }, 
				{ 1 }, 
				{ 1 }, 
				{ 0, 5 }, 
				{ 4, 6 }, 
				{ 5 }, 
				{ 0 } 
			});
		
		int DISCOVERY_MEMBERSHIP_ID = builder.assignLinkable(new long[][] { {},
				{}, {}, {}, {}, {}, {}, {} });
		
		int NEIGHBOR_ID = builder.assignCompleteLinkable();

		// Schedule covers the tree, generating a duplicate at the last
		// round to put the protocol into quiescence at the leafs.
		int SELECTOR_ID = DeterministicSelector.assignSchedule(builder, MEMBERSHIP_ID,
				new Long[][] { 
				{1L, 	4L,		7L,		null,	null}, 
				{null,	2L,		3L,		null,	null},
				{null,	null,	null,	null,	1L	}, 
				{null,	null,	null,	null,	1L	},
				{null,	null,	5L,		null,	null}, 
				{null,	null,	null,	6L 	,	null},
				{null,	null,	null,	null,	5L	},
				{null,	null,	null,	null,	0L	} 
			});
		
		ByteArrayOutputStream log = new ByteArrayOutputStream();

		TableWriter logWriter = tableWriter(
				new TeeOutputStream(log, System.out), DiscoveryProtocol.class);
		builder.done();
		
		int DISCOVERY_PID = configureProtocols(builder, SELECTOR_ID,
				MEMBERSHIP_ID, DISCOVERY_MEMBERSHIP_ID, NEIGHBOR_ID, logWriter);

		Node zero = builder.getNodes().get(0);
		DiscoveryProtocol protocol = (DiscoveryProtocol) zero
				.getProtocol(DISCOVERY_PID);
		protocol.reinitialize();
		
		FakeCycleEngine engine = new FakeCycleEngine(builder.getNodes(),
				System.currentTimeMillis());
		engine.run(8);
		
		TabularLogMatcher matcher = new TabularLogMatcher(logWriter.fields());

		// Round 1.
		matcher.add(DiscoveryProtocol.SF_CREATE, 0, 1, 0);
		
		// Round 2.
		matcher.add(DiscoveryProtocol.SF_CREATE, 0, 4, 0);
		matcher.add(DiscoveryProtocol.SF_CREATE, 1, 2, 0);
		
		// Round 3.
		matcher.add(DiscoveryProtocol.SF_CREATE, 0, 7, 0);
		matcher.add(DiscoveryProtocol.LEAF, "none", 7, "none");
		
		matcher.add(DiscoveryProtocol.SF_CREATE, 1, 3, 0);
		matcher.add(DiscoveryProtocol.SF_CREATE, 4, 5, 0);
		
		matcher.add(DiscoveryProtocol.TRANSFER, 7, 0, 1);
		
		// Round 4.
		matcher.add(DiscoveryProtocol.SF_CREATE, 5, 6, 0);
		
		// Round 5, messages dropped, aggregation begins.
		matcher.add(DiscoveryProtocol.LEAF, "none", 2, "none");
		matcher.add(DiscoveryProtocol.TRANSFER, 2, 1, 1);
		
		matcher.add(DiscoveryProtocol.LEAF, "none", 3, "none");
		matcher.add(DiscoveryProtocol.TRANSFER, 3, 1, 1);
		
		matcher.add(DiscoveryProtocol.LEAF, "none", 6, "none");
		matcher.add(DiscoveryProtocol.TRANSFER, 6, 5, 1);
		
		// Round 6, aggregation proceeds.
		matcher.add(DiscoveryProtocol.TRANSFER, 1, 0, 3);
		matcher.add(DiscoveryProtocol.TRANSFER, 5, 4, 2);
		
		// Round 7, aggregation proceeds.
		matcher.add(DiscoveryProtocol.TRANSFER, 4, 0, 3);
		
		matcher.match(new ByteArrayInputStream(log.toByteArray()));
	}

	@SuppressWarnings("unchecked")
	private int configureProtocols(TestNetworkBuilder builder, int selector,
			int membership, int discoveryMembership, int neighbors, TableWriter log) {
		int dpid = -1;
		for (Node node : builder.getNodes()) {
			int mcast_id = builder.nextProtocolId(node);
			dpid = mcast_id + 1; 

			// Rumor mongering protocol.
			DemersRumorMonger drm = new DemersRumorMonger(GIVEUP,
					Integer.MAX_VALUE, mcast_id, node,
					(Linkable) node.getProtocol(membership), fRand, false);
			
			// Discovery protocol.
			DiscoveryProtocol dp = new DiscoveryProtocol(dpid, neighbors,
					neighbors, discoveryMembership, mcast_id, Integer.MAX_VALUE, Integer.MAX_VALUE, log);
			dp.initialize(node);
			
			// Multicast service.
			MulticastService sns = new MulticastService(null, mcast_id, new NullConfigurator());

			sns.initialize(node);
			sns.flushState();
			sns.setStorage(new SimpleEventStorage());
			sns.addStrategy(new Class[] { DemersRumorMonger.class }, drm,
					new ProtocolReference<IPeerSelector>(selector),
					new FallThroughReference<ISelectionFilter>(
							ISelectionFilter.ALWAYS_TRUE_FILTER));
			sns.addSubscriber(drm);
			sns.addSubscriber(dp);
			sns.compact();
			
			builder.addProtocol(node, sns);
			builder.addProtocol(node, dp);
		}
		return dpid;
	}
}
