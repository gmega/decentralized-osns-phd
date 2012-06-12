package it.unitn.disi.churn.diffusion.graph;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.SparseIDMapper;
import it.unitn.disi.utils.collections.Triplet;

public class LiveTransformer implements ILiveTransformer {

	@Override
	public Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> live(
			IndexedNeighborGraph source, INetwork network) {
		
		// Handles special cases.
		if (network.live() == 0) {
			return NO_LIVE_PEER;
		}
		
		if (network.live() == source.size()) {
			return null;
		}

		// Establishes who the live vertices are, and remaps their ID.
		AbstractIDMapper mapper = map(source, network);

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

		return new Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph>(
				mapper, new MappingNetwork(mapper, network),
				LightweightStaticGraph.fromAdjacency(adjacencies));
	}

	private AbstractIDMapper map(IndexedNeighborGraph source, INetwork network) {
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

	private static class MappingNetwork implements INetwork {

		private final AbstractIDMapper fMapper;

		private final INetwork fNetwork;

		private final double fVersion;

		public MappingNetwork(AbstractIDMapper mapper, INetwork network) {
			fMapper = mapper;
			fNetwork = network;
			fVersion = network.version();
		}

		@Override
		public int size() {
			checkVersion();
			return fMapper.size();
		}

		@Override
		public IProcess process(int index) {
			checkVersion();
			return fNetwork.process(fMapper.reverseMap(index));
		}

		@Override
		public double version() {
			return fVersion;
		}

		@Override
		public int live() {
			checkVersion();
			return fNetwork.live();
		}

		private void checkVersion() {
			if (fVersion != fNetwork.version()) {
				throw new IllegalStateException();
			}
		}

	}
}
