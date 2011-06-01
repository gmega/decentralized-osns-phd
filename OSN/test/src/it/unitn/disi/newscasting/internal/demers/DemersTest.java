package it.unitn.disi.newscasting.internal.demers;

import java.util.Random;

import org.junit.Test;

import junit.framework.Assert;

import peersim.config.IResolver;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.newscasting.IContentExchangeStrategy.ActivityStatus;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.newscasting.internal.SimpleEventStorage;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.newscasting.internal.selectors.RandomSelectorOverLinkable;
import it.unitn.disi.test.framework.FakeCycleEngine;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.peersim.SNNode;

public class DemersTest extends PeerSimTest {

	private static final IApplicationConfigurator NULL_CONFIGURATOR = new IApplicationConfigurator() {
		@Override
		public void configure(IProtocolSet app, IResolver resolver,
				String prefix) throws Exception {

		}

		public Object clone() {
			return this;
		}
	};

	private final Random fRand = new Random(42);

	private int fLinkableId;

	private int fProtocolId;

	@Test
	public void residueDuplicateTest() {
		double[] p = { 1.0, 0.5, 0.33, 0.25, 0.2 };
		double[] r = { 0.96, 0.205, 0.060, 0.021, 0.008 };

		final int REPS = 10;
		final double DUP_EPSILON = 0.4;

		for (int i = 0; i < p.length; i++) {
			double residue = 0.0;
			DupCounter counter = new DupCounter();
			for (int j = 0; j < REPS; j++) {
				residue += runTest(p[i], counter);
			}
			residue /= REPS;
			double dups = ((double) counter.duplicates()) / (1000.0 * REPS);
			System.err.println("Residue for p = " + p[i] + " is " + residue
					+ ", dups are " + dups + ".");
			Assert.assertTrue(residue <= r[i]);
			Assert.assertTrue(Math.abs(1.0 / p[i] - dups) <= DUP_EPSILON);
		}
	}

	private double runTest(double p, DupCounter counter) {
		TestNetworkBuilder builder = configureCompleteNetwork(1000);
		configureSocialNewscasting(builder, counter, p);

		// Takes random node and posts a tweet.
		Node node = builder.getNodes().get(fRand.nextInt(builder.size()));
		SocialNewscastingService sns = (SocialNewscastingService) node
				.getProtocol(fProtocolId);
		Tweet tweet = sns.postToFriends();

		FakeCycleEngine engine = new FakeCycleEngine(builder.getNodes(),
				System.currentTimeMillis());
		engine.run(1000);

		assertQuiescence(builder);
		return computeResidue(builder, tweet);
	}

	@SuppressWarnings("unchecked")
	private void configureSocialNewscasting(TestNetworkBuilder builder,
			DupCounter counter, double p) {
		for (Node node : builder.getNodes()) {
			fProtocolId = builder.nextProtocolId(node);

			// Rumor mongering protocol.
			DemersRumorMonger drm = new DemersRumorMonger(p, Integer.MAX_VALUE,
					fProtocolId, node,
					(Linkable) node.getProtocol(fLinkableId), fRand, false);

			// Selector.
			RandomSelectorOverLinkable selector = new RandomSelectorOverLinkable(
					fLinkableId);

			// Social Newscasting service.
			SocialNewscastingService sns = new SocialNewscastingService(null,
					builder.nextProtocolId(node), fLinkableId,
					NULL_CONFIGURATOR);

			sns.initialize(node);
			sns.flushState();
			sns.setStorage(new SimpleEventStorage());
			sns.addStrategy(new Class[] { DemersRumorMonger.class }, drm,
					new FallThroughReference<IPeerSelector>(selector),
					new FallThroughReference<ISelectionFilter>(
							ISelectionFilter.ALWAYS_TRUE_FILTER));
			sns.addSubscriber(drm);
			sns.addSubscriber(counter);
			sns.compact();

			builder.addProtocol(node, sns);
		}
	}

	private void assertQuiescence(TestNetworkBuilder builder) {
		for (Node node : builder.getNodes()) {
			SocialNewscastingService sns = (SocialNewscastingService) node
					.getProtocol(fProtocolId);
			DemersRumorMonger drm = sns.getStrategy(DemersRumorMonger.class);
			Assert.assertEquals(ActivityStatus.QUIESCENT, drm.status());
		}
	}

	private double computeResidue(TestNetworkBuilder builder, Tweet tweet) {
		int total = builder.size();
		int residue = builder.size();
		for (Node node : builder.getNodes()) {
			SocialNewscastingService sns = (SocialNewscastingService) node
					.getProtocol(fProtocolId);
			if (sns.storage().contains(tweet)) {
				residue--;
			}
		}

		double aresidue = (double) (residue) / total;
		return aresidue;
	}

	private TestNetworkBuilder configureCompleteNetwork(int i) {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(1000);
		fLinkableId = builder.assignCompleteLinkable();

		return builder;
	}

	class DupCounter implements IEventObserver {

		private int fDups;

		public DupCounter() {
		}

		@Override
		public void localDelivered(IGossipMessage message) {
		}

		@Override
		public void delivered(SNNode sender, SNNode receiver,
				IGossipMessage message, boolean duplicate) {
			if (duplicate) {
				fDups++;
			}
		}

		public int duplicates() {
			return fDups;
		}

	}
}
