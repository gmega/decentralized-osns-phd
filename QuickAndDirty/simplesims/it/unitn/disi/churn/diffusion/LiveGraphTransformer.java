package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.SparseIDMapper;
import it.unitn.disi.utils.collections.Pair;

public class LiveGraphTransformer implements ILiveGraphTransformer {

	@Override
	public Pair<AbstractIDMapper, IndexedNeighborGraph> liveGraph(
			IndexedNeighborGraph source, INetwork network) {

		// Establishes who the live vertices are, and remaps their ID.
		AbstractIDMapper mapper = map(source, network);
		if (mapper == null) {
			return null;
		}

		// Now remaps the edges that start from nodes that are alive, trimming
		// out the ones pointing to nodes that are currently down.
		int[][] adjacencies = new int[mapper.size()][];
		TIntArrayList neighbors = new TIntArrayList();

		for (int i = 0; i < adjacencies.length; i++) {
			neighbors.resetQuick();

			int vertex = mapper.reverseMap(i);
			for (int j = 0; j < source.degree(vertex); j++) {
				int neighbor = source.getNeighbor(vertex, j);
				// Is it alive?
				if (network.process(neighbor).isUp()) {
					neighbors.add(mapper.map(neighbor));
				}
			}

			adjacencies[i] = neighbors.toArray();
		}

		return new Pair<AbstractIDMapper, IndexedNeighborGraph>(mapper,
				LightweightStaticGraph.fromAdjacency(adjacencies));
	}

	private AbstractIDMapper map(IndexedNeighborGraph source,
			INetwork network) {
		SparseIDMapper mapper = null;
		for (int i = 0; i < source.size(); i++) {
			if (network.process(i).isUp()) {
				if (mapper == null) {
					mapper = new SparseIDMapper();
				}
				mapper.addMapping(i);
			}
		}
		return mapper;
	}

}
