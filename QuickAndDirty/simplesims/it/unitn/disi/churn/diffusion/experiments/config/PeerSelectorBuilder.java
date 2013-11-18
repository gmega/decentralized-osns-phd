package it.unitn.disi.churn.diffusion.experiments.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import it.unitn.disi.churn.config.FastRandomAssignmentReader;
import it.unitn.disi.churn.diffusion.BalancingSelector;
import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.RandomSelector;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.utils.MiscUtils;

/**
 * {@link PeerSelectorBuilder} is a helper class with a half-assed fluent
 * interface for helping construct our ever more complicated peer selectors. :-)
 * 
 * @author giuliano
 */
public class PeerSelectorBuilder {

	private IPeerSelector[] fLast;

	private Random fRandom;

	private IGraphProvider fProvider;

	private int fEgonetId;

	public PeerSelectorBuilder(IGraphProvider provider, Random random, int egoId) {
		fRandom = random;
		fProvider = provider;
		fEgonetId = egoId;
	}

	/**
	 * @return a builder for selectors that pick nodes uniformly at random.
	 */
	public StatelessSelectorBuilder useRandom() throws IOException {
		return new StatelessSelectorBuilder(new RandomSelector(fRandom),
				egoSize());
	}

	/**
	 * @return a builder for selectors that pick nodes with a probability that
	 *         is inveresely proportional to their degree.
	 */
	public StatelessSelectorBuilder useAnticentrality() throws IOException {
		return new StatelessSelectorBuilder(new BiasedCentralitySelector(
				fRandom, true), egoSize());
	}

	/**
	 * @return a builder for selectors that pick nodes with a probability that
	 *         is proportional to their degree.
	 */
	public StatelessSelectorBuilder useCentrality() throws IOException {
		return new StatelessSelectorBuilder(new BiasedCentralitySelector(
				fRandom, false), egoSize());
	}

	/**
	 * @return a builder for balanced selectors that can honour both inbound and
	 *         outbound bandwidth caps.
	 * 
	 * @param egonet
	 *            the id of the ego network in which the selectors will be
	 *            employed.
	 * @param provider
	 *            an {@link IGraphProvider} from which to access the (complete)
	 *            neighborhoods of all nodes in the ego network, as well as the
	 *            ego network itself.
	 * 
	 * @throws IOException
	 *             if there are problems reading the graph.
	 */
	public BalancedSelectorBuilder useBalanced() throws IOException {
		return new BalancedSelectorBuilder();
	}

	public IPeerSelector[] lastResults() {
		if (fLast == null) {
			throw new IllegalStateException("Nothing has been built.");
		}
		return fLast;
	}

	private int egoSize() throws IOException {
		return fProvider.size(fEgonetId);
	}

	/**
	 * "Constructs" stateless selectors. Since they are stateless, they can
	 * simply be flyweighted all over the array.
	 * 
	 * @author giuliano
	 */
	public class StatelessSelectorBuilder {

		private IPeerSelector fSelector;

		private int fSize;

		public StatelessSelectorBuilder(IPeerSelector selector, int size) {
			fSelector = selector;
			fSize = size;
		}

		public IPeerSelector[] andGetSelectors() {
			IPeerSelector[] selectors = new IPeerSelector[fSize];
			for (int i = 0; i < selectors.length; i++) {
				selectors[i] = fSelector;
			}
			fLast = selectors;
			return fLast;
		}

	}

	/**
	 * Constructs {@link BalancingSelector}s, allowing for several parameters to
	 * be tuned more easily.
	 * 
	 * @author giuliano
	 */
	public class BalancedSelectorBuilder {

		private IndexedNeighborGraph fGraph;

		private int[] fEgoDegrees = null;

		private int[] fSocialDegrees = null;

		private double[] fInboundBandwidth = null;

		private double[] fOutboundBandwidth = null;

		private BalancedSelectorBuilder() throws IOException {
			fGraph = fProvider.subgraph(fEgonetId);
		}

		/**
		 * Set selectors to use dynamic approximations for ego network degrees.
		 * If not called, the node's static degrees will be used instead.
		 * 
		 * @index the indexed availabilities for nodes in the graph.
		 * 
		 * @throws IOException
		 *             if thrown by the underlying {@link IGraphProvider} or
		 *             {@link FastRandomAssignmentReader}.
		 */
		public BalancedSelectorBuilder withDynamicEgoDegrees(
				FastRandomAssignmentReader index) throws IOException {
			int[] ids = fProvider.verticesOf(fEgonetId);
			fEgoDegrees = new int[fGraph.size()];

			for (int i = 0; i < ids.length; i++) {
				double degree = 0.0;
				int staticDegree = fGraph.degree(i);
				for (int j = 0; j < staticDegree; j++) {
					int neighbor = fGraph.getNeighbor(i, j);
					degree += index.ai(ids[neighbor]);
				}
				fEgoDegrees[i] = round(degree);
			}

			return this;
		}

		/**
		 * Make selectors use dynamic approximations for social network degrees.
		 * If not called, the node's static degrees will be used instead.
		 * 
		 * @index the indexed availabilities for nodes in the graph.
		 * 
		 * @throws IOException
		 *             if thrown by the underlying {@link IGraphProvider} or
		 *             {@link FastRandomAssignmentReader}.
		 */
		public BalancedSelectorBuilder withDynamicSocialDegrees(
				FastRandomAssignmentReader index) throws IOException {
			int ids[] = fProvider.verticesOf(fEgonetId);
			int[] fSocialDegrees = new int[ids.length];
			for (int i = 0; i < fSocialDegrees.length; i++) {
				fSocialDegrees[i] = dynamicDegree(ids[i], fProvider, index);
			}

			return this;
		}

		/**
		 * Make selectors use the static (graph-structural) degree of a node as
		 * its actual social degree.
		 * 
		 * @throws IOException
		 *             if there are problems accessing graph components.
		 */
		public BalancedSelectorBuilder withStaticSocialDegrees()
				throws IOException {
			int ids[] = fProvider.verticesOf(fEgonetId);
			fSocialDegrees = new int[ids.length];
			for (int i = 0; i < ids.length; i++) {
				fSocialDegrees[i] = fProvider.size(ids[i]) - 1;
			}

			return this;
		}

		/**
		 * Make selectors use the static (graph-structural) degree of a node as
		 * its actual ego network degree.
		 * 
		 * @throws IOException
		 *             if there are problems accessing graph components.
		 */
		public BalancedSelectorBuilder withStaticEgoDegrees() {
			fEgoDegrees = new int[fGraph.size()];
			for (int i = 0; i < fEgoDegrees.length; i++) {
				fEgoDegrees[i] = fGraph.degree(i);
			}

			return this;
		}

		/**
		 * Sets everybody's outbound bandwidth cap to the same value.
		 * 
		 * @param cap
		 *            the global cap to use.
		 */
		public BalancedSelectorBuilder globalOutboundCap(double cap) {
			fOutboundBandwidth = filledArray(cap);

			return this;
		}

		/**
		 * Sets everybody's inbound bandwidth cap to the same value.
		 * 
		 * @param cap
		 *            the global cap to use.
		 * 
		 */
		public BalancedSelectorBuilder globalInboundCap(double cap) {
			fInboundBandwidth = filledArray(cap);

			return this;
		}

		/**
		 * Sets everyone's outbound bandwidth caps so that at least a certain
		 * amount is guaranteed for each friend. This means the maximum allotted
		 * <i>outbound</i> bandwidth for node v will be: <code>
		 * 	cap * degree(v)
		 * </code>
		 * 
		 * @param cap
		 *            the cap to be honoured per friend.
		 */
		public BalancedSelectorBuilder perFriendOutboundCap(double cap)
				throws IOException {
			ensureDegrees();
			fOutboundBandwidth = scaledArray(fSocialDegrees, cap);

			return this;
		}

		/**
		 * Sets everyone's inbound bandwidth caps so that at least a certain
		 * amount is guaranteed for each friend. This means the maximum allotted
		 * <i>inbound</i> bandwidth for node v will be: <code>
		 * 	cap * degree(v)
		 * </code>
		 * 
		 * @param cap
		 *            the cap to be honoured per friend.
		 */
		public BalancedSelectorBuilder perFriendInboundCap(double cap)
				throws IOException {
			ensureDegrees();
			fInboundBandwidth = scaledArray(fSocialDegrees, cap);

			return this;
		}

		/**
		 * Sets the inbound bandwidth of every node v to a max(cap, inbound[v]),
		 * where inbound[v] is the bandwidth assignment for v produced by a
		 * previous call.
		 * 
		 * @param cap
		 *            the lower bound for outbound bandwidth cap.
		 * @return
		 */
		public BalancedSelectorBuilder andLowerInboundCap(double cap) {
			ensureBdwCaps();
			setMinimumBdw(fInboundBandwidth, cap);
			
			return this;
		}
		
		/**
		 * Sets the outbound bandwidth of every node v to max(cap, outbound[v]),
		 * where outbound[v] is the bandwidth assignment for v produced by a
		 * previous call.
		 * 
		 * @param cap
		 *            the lower bound for outbound bandwidth cap.
		 * @return
		 */
		public BalancedSelectorBuilder andLowerOutboundCap(double cap) {
			ensureBdwCaps();
			setMinimumBdw(fOutboundBandwidth, cap);
			
			return this;
		}

		public IPeerSelector[] andGetSelectors() throws IOException {
			ensureDegrees();
			ensureBdwCaps();
			fLast = BalancingSelector.degreeApproximationSelectors(fRandom,
					fEgoDegrees, fSocialDegrees, fOutboundBandwidth,
					fInboundBandwidth);

			return fLast;
		}

		private void setMinimumBdw(double[] assignment, double cap) {
			for (int i = 0; i < assignment.length; i++) {
				assignment[i] = Math.max(cap, assignment[i]);
			}
		}

		private double[] filledArray(double cap) {
			double[] array = new double[fGraph.size()];
			Arrays.fill(array, cap);
			return array;
		}

		private double[] scaledArray(int[] original, double scaling) {
			double[] array = new double[original.length];
			for (int i = 0; i < array.length; i++) {
				array[i] = original[i] * scaling;
			}
			return array;
		}

		private void ensureDegrees() throws IOException {
			if (fEgoDegrees == null) {
				withStaticEgoDegrees();
			}

			if (fSocialDegrees == null) {
				withStaticSocialDegrees();
			}
		}

		private void ensureBdwCaps() {
			if (fInboundBandwidth == null || fOutboundBandwidth == null) {
				throw new IllegalStateException(
						"Bandwidth caps have not yet been set.");
			}
		}

		private int round(double degree) {
			return MiscUtils.safeCast(Math.max(1, Math.round(degree)));
		}

		private int dynamicDegree(int id, IGraphProvider provider,
				FastRandomAssignmentReader index) throws IOException {
			int[] ids = provider.verticesOf(id);
			double degree = 0.0;
			for (int i = 1; i < ids.length; i++) {
				degree += index.ai(ids[i]);
			}

			return round(degree);
		}
	}
}
