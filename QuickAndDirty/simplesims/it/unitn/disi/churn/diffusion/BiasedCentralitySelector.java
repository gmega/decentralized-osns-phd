package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.util.RouletteWheel;

import java.util.BitSet;

public class BiasedCentralitySelector implements IPeerSelector{

	private final TDoubleArrayList fWeights = new TDoubleArrayList(1000);
	
	private final TIntArrayList fIndices = new TIntArrayList(1000);
	
	@Override
	public int selectPeer(int root, IndexedNeighborGraph neighbors,
			BitSet forbidden, SimpleEDSim sim) {
		RouletteWheel wheel = rouletteWheel(root, neighbors, forbidden, sim);
		return wheel.spin();
	}

	private RouletteWheel rouletteWheel(int root,
			IndexedNeighborGraph neighbors, BitSet forbidden, SimpleEDSim sim) {
		return null;
	}

}
