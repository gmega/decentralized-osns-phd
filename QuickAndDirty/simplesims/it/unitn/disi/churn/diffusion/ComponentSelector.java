package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.TarjanState;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.core.INetwork;

import java.util.BitSet;
import java.util.Random;

public class ComponentSelector implements IPeerSelector {

	private TarjanState fState = new TarjanState();

	private BiasedCentralitySelector fAnticentrality;

	private BitSet fAllForbidden = new BitSet();

	private BitSet fComponent = new BitSet();

	public ComponentSelector(Random rnd) {
		fAnticentrality = new BiasedCentralitySelector(rnd, true);
	}

	@Override
	public int selectPeer(int selecting, IndexedNeighborGraph graph,
			BitSet forbidden, INetwork sim) {
		// Gets the subgraph composed by the nodes reachable from the selecting
		// node, minus the selecting node itself, and the nodes already reached
		// by the message.
		int[] excluded = collectExcluded(forbidden, selecting);
		LightweightStaticGraph subgraph = LightweightStaticGraph.subgraph(
				(LightweightStaticGraph) graph, excluded);

		// Computes components.
		GraphAlgorithms.tarjan(fState, subgraph);

		// Selects the largest component.
		TIntArrayList biggest = fState.components.get(0);
		for (TIntArrayList candidate : fState.components) {
			if (candidate.size() > biggest.size()) {
				biggest = candidate;
			}
		}

		// Now, forbids access to all nodes that are not in the selected
		// component.
		toBitSet(biggest, fComponent, excluded);
		allowOnly(fComponent, graph);

		// Picks a node in using anticentrality.
		return fAnticentrality.selectPeer(selecting, graph, fAllForbidden, sim);

	}

	private void toBitSet(TIntArrayList biggest, BitSet set, int [] excluded) {
		for (int i = 0; i < biggest.size(); i++) {
			set.set(excluded[biggest.get(i)]);
		}
	}

	private void allowOnly(BitSet biggest, IndexedNeighborGraph graph) {
		fAllForbidden.clear();
		for (int i = 0; i < graph.size(); i++) {
			if (!biggest.get(i)) {
				fAllForbidden.set(i);
			}
		}
	}

	private int[] collectExcluded(BitSet forbidden, int selecting) {
		TIntArrayList excluded = new TIntArrayList();
		excluded.add(selecting);

		for (int i = forbidden.nextSetBit(0); i >= 0; i = forbidden
				.nextSetBit(i)) {
			excluded.add(i);
		}

		return excluded.toArray();
	}

}
