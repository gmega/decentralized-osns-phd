package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.collections.Pair;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.Random;

public class BiasedCentralitySelector implements IPeerSelector {

	private final Random fRandom;

	private final boolean fAnticentrality;

	public BiasedCentralitySelector(Random random, boolean anticentrality) {
		fRandom = random;
		fAnticentrality = anticentrality;
	}

	@Override
	public int selectPeer(int root, IndexedNeighborGraph graph,
			BitSet allForbidden, INetwork sim) {

		// Excludes the forbidden nodes that are not neighbors of
		// the root.
		BitSet forbidden = forbidden(allForbidden, root, graph);

		if (graph.degree(root) - forbidden.cardinality() == 0) {
			return IPeerSelector.NO_PEER;
		}

		Pair<Pair<Integer, Double>[], RouletteWheel> pair = rouletteWheel(root,
				graph, forbidden, sim);

		if (pair == null) {
			return IPeerSelector.NO_LIVE_PEER;
		}

		return pair.a[pair.b.spin()].a;
	}

	private BitSet forbidden(BitSet allForbidden, int root,
			IndexedNeighborGraph graph) {
		BitSet forbidden = new BitSet();
		for (int i = 0; i < graph.degree(root); i++) {
			forbidden.set(graph.getNeighbor(root, i));
		}
		forbidden.and(allForbidden);
		return forbidden;
	}

	private Pair<Pair<Integer, Double>[], RouletteWheel> rouletteWheel(
			int root, IndexedNeighborGraph graph, BitSet forbidden, INetwork sim) {

		int eligible = graph.degree(root) - forbidden.cardinality();

		@SuppressWarnings("unchecked")
		Pair<Integer, Double>[] neighbors = (Pair<Integer, Double>[]) new Pair[eligible];

		double total = 0;
		int k = -1;
		for (int i = 0; i < graph.degree(root); i++) {
			// Takes into account only these nodes in the graph that are not
			// forbidden.
			int neighbor = graph.getNeighbor(root, i);
			if (!forbidden.get(neighbor)) {
				neighbors[++k] = new Pair<Integer, Double>(neighbor,
						centrality(root, graph, neighbor));
				total += neighbors[k].b;
			}
		}

		if (fAnticentrality) {
			Arrays.sort(neighbors, new Comparator<Pair<Integer, Double>>() {
				@Override
				public int compare(Pair<Integer, Double> o1,
						Pair<Integer, Double> o2) {
					return (int) Math.signum(o1.b - o2.b);
				}
			});
		}

		double[] weights = new double[neighbors.length];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = neighbors[fAnticentrality ? neighbors.length - i - 1
					: i].b / total;
		}

		return new Pair<Pair<Integer, Double>[], RouletteWheel>(neighbors,
				new RouletteWheel(weights, fRandom));
	}

	protected Double centrality(int root, IndexedNeighborGraph graph,
			int neighbor) {
		return (double) graph.degree(neighbor);
	}
}
