package it.unitn.disi.f2f;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import junit.framework.Assert;

import it.unitn.disi.NullConfigurator;
import it.unitn.disi.epidemics.ISelectionFilter;
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

	private int fDemersMembership;

	private int fDiscoveryMembership;

	private int fDiscoveryProtocol;

	private int fNeighborId;

	private int fSelectorId;

	@Test
	public void testSimpleCollect() throws IOException {
		TestNetworkBuilder builder = simpleCollectFixture();
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		TableWriter logWriter = tableWriter(
				new TeeOutputStream(log, System.out), DiscoveryProtocol.class);
		configureProtocols(builder, fSelectorId, fDemersMembership,
				fDiscoveryMembership, fNeighborId, fNeighborId, logWriter);
		TabularLogMatcher matcher = runTest(builder, 8, logWriter);

		Node zero = builder.getNodes().get(0);

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

		this.assertKnowsAll(fDiscoveryProtocol, fNeighborId, zero);
	}

	@Test
	public void testSparseCollect() throws Exception {
		ByteArrayOutputStream log = new ByteArrayOutputStream();
		TableWriter logWriter = tableWriter(
				new TeeOutputStream(log, System.out), DiscoveryProtocol.class);
		TestNetworkBuilder builder = sparseCollectFixture(logWriter);
		TabularLogMatcher matcher = runTest(builder, 11, logWriter);
		
		
		// 1
		matcher.add(DiscoveryProtocol.SF_CREATE, 0, 6, 0);
		
		// 2
		matcher.add(DiscoveryProtocol.SF_CREATE, 6, 8, 0);
		
		// 3
		matcher.add(DiscoveryProtocol.LEAF, "none", 8, "none");
		matcher.add(DiscoveryProtocol.TRANSFER, 8, 6, 1);
		
		// 4
		matcher.add(DiscoveryProtocol.TRANSFER, 6, 0, 1);
		matcher.add(DiscoveryProtocol.SF_CREATE, 6, 7, 0);
		
		matcher.add(DiscoveryProtocol.SF_CREATE, 6, 9, 0);
		matcher.add(DiscoveryProtocol.LEAF, "none", 7, "none");
		matcher.add(DiscoveryProtocol.TRANSFER, 7, 6, 1);
		matcher.add(DiscoveryProtocol.TRANSFER, 6, 0, 1);
		
		matcher.add(DiscoveryProtocol.SF_CREATE, 9, 10, 0);
		
		matcher.add(DiscoveryProtocol.LEAF, "none", 10, "none");
		matcher.add(DiscoveryProtocol.TRANSFER, 10, 9, 1);
		
		matcher.add(DiscoveryProtocol.TRANSFER, 9, 6, 4);
		matcher.add(DiscoveryProtocol.TRANSFER, 6, 0, 3);
		
		matcher.match(new ByteArrayInputStream(log.toByteArray()));
	}

	public TabularLogMatcher runTest(TestNetworkBuilder builder, int cycles,
			TableWriter logWriter) {
		Node zero = builder.getNodes().get(0);
		DiscoveryProtocol protocol = (DiscoveryProtocol) zero
				.getProtocol(fDiscoveryProtocol);
		protocol.reinitialize();

		FakeCycleEngine engine = new FakeCycleEngine(builder.getNodes(),
				System.currentTimeMillis());
		engine.run(cycles);

		return new TabularLogMatcher(logWriter.fields());
	}

	private TestNetworkBuilder simpleCollectFixture() {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(8);

		fDemersMembership = builder.assignLinkable(new long[][] { 
				{ 1, 4, 7 },
				{ 0, 2, 3 }, 
				{ 1 }, 
				{ 1 }, 
				{ 0, 5 }, 
				{ 4, 6 }, 
				{ 5 }, 
				{ 0 } 
			});

		fDiscoveryMembership = builder.assignLinkable(new long[][] { {}, {},
				{}, {}, {}, {}, {}, {} });

		fNeighborId = builder.assignCompleteLinkable();

		// Schedule covers the tree, generating a duplicate at the last
		// round to put the protocol into quiescence at the leafs.
		fSelectorId = DeterministicSelector.assignSchedule(builder,
				fDemersMembership, new Long[][] { 
						{1L,	4L,		7L,		null,	null},
						{null,	2L, 	3L,		null,	null},
						{null,	null,	null,	null,	1L	},
						{null,	null,	null,	null,	1L	},
						{null,	null,	5L,		null,	null},
						{null,	null,	null,	6L,		null},
						{null,	null,	null,	null,	5L	},
						{null,	null,	null,	null,	0L	} 
					});

		return builder;
	}
	
	private TestNetworkBuilder sparseCollectFixture(TableWriter log) {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(11);

		fDemersMembership = builder.assignLinkable(new long[][] { 
				{ 6 },
				{},
				{},
				{},
				{},
				{},
				{0, 7, 8, 9},
				{6},
				{6},
				{6, 10},
				{9}
			});

		fDiscoveryMembership = builder.assignLinkable(new long[][] { {}, {},
				{}, {}, {}, {}, {}, {}, {}, {}, {} });

		fNeighborId = builder.assignLinkable(new long [][] {
				{1, 2, 3, 4, 5},
				{},
				{},
				{},
				{},
				{},
				{7, 8, 9},
				{6, 1, 3},
				{6, 3},
				{6, 1, 2, 3, 4, 10},
				{9, 2, 4, 5}
		});
		
		int VISIBILITY_ID = builder.assignCompleteLinkable();

		fSelectorId = DeterministicSelector.assignSchedule(builder,
				fDemersMembership, new Long[][] { 
						{6L,	null,	null,	null,	null,	null,	null,	null,	null,	null,	null},
						{null,	null, 	null,	null,	null,	null,	null,	null,	null,	null,	null},
						{null,	null, 	null,	null,	null,	null,	null,	null,	null,	null,	null},
						{null,	null, 	null,	null,	null,	null,	null,	null,	null,	null,	null},
						{null,	null, 	null,	null,	null,	null,	null,	null,	null,	null,	null},
						{null,	null, 	null,	null,	null,	null,	null,	null,	null,	null,	null},
						{null,	8L, 	null, 	null, 	7L,		9L,		null,	null,	null,	null,	null},
						{null,	null,	null,	null,	null,	6L,		null,	null,	null,	null,	null},
						{null,	null,	6L,		null,	null,	null,	null,	null,	null,	null,	null},
						{null,	null, 	null,	null,	null,	null,	10L,	null,	null,	null,	null},
						{null,	null, 	null,	null,	null,	null,	null,	9L,		null,	null,	null}
					});

		configureProtocols(builder, fSelectorId, fDemersMembership,
				fDiscoveryMembership, fNeighborId, VISIBILITY_ID, log);
		
		// Bootstraps the discovery protocols.
		// 7 knows 1 and 3.
		DiscoveryProtocol p7 = getD(builder, 7);
		knows(builder, p7, 1, 3);
		
		DiscoveryProtocol p8 = getD(builder, 8);
		knows(builder, p8, 3);
		
		DiscoveryProtocol p9 = getD(builder, 9);
		knows(builder, p9, 1, 2, 3, 4);
		
		DiscoveryProtocol p10 = getD(builder, 10);
		knows(builder, p10, 2, 4, 5);
		
		return builder;	
	}

	private void knows(TestNetworkBuilder builder, DiscoveryProtocol disc, int...neis) {
		for (int nei : neis) {
			disc.addNeighbor(builder.getNodes().get(nei));
		}
	}

	private DiscoveryProtocol getD(TestNetworkBuilder builder, int i) {
		return (DiscoveryProtocol) builder.getNodes().get(i)
				.getProtocol(fDiscoveryProtocol);
	}

	private void assertKnowsAll(int protocolId, int neighborId, Node... nodes) {
		for (Node node : nodes) {
			Linkable neighbors = (Linkable) node.getProtocol(neighborId);
			DiscoveryProtocol protocol = (DiscoveryProtocol) node
					.getProtocol(protocolId);
			for (int i = 0; i < neighbors.degree(); i++) {
				Assert.assertTrue(protocol.contains(neighbors.getNeighbor(i)));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void configureProtocols(TestNetworkBuilder builder, int selector,
			int membership, int discoveryMembership, int neighbors, int visibility,
			TableWriter log) {

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
					visibility, discoveryMembership, mcast_id,
					Integer.MAX_VALUE, Integer.MAX_VALUE, log);
			dp.initialize(node);

			// Multicast service.
			MulticastService sns = new MulticastService(null, mcast_id,
					new NullConfigurator());

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
		fDiscoveryProtocol = dpid;
	}
}
