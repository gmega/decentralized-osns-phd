package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.graph.IndexedNeighborGraph;

import java.util.BitSet;
import java.util.Random;

public class RandomSelector implements IPeerSelector {

	private TIntArrayList fNeighbors = new TIntArrayList(1000);

	private Random fRandom;

	public RandomSelector(Random random) {
		fRandom = random;
	}

	@Override
	public int selectPeer(int root, IndexedNeighborGraph neighbors,
			BitSet forbidden, INetwork sim) {

		fNeighbors.resetQuick();
		for (int i = 0; i < neighbors.degree(root); i++) {
			int neighbor = neighbors.getNeighbor(root, i);
			if (!forbidden.get(neighbor)) {
				fNeighbors.add(neighbor);
			}
		}

		if (fNeighbors.size() == 0) {
			return NO_PEER;
		}

		fNeighbors.shuffle(fRandom);

		for (int i = 0; i < fNeighbors.size(); i++) {
			if (sim.process(fNeighbors.get(i)).isUp()) {
				return fNeighbors.get(i);
			}
		}

		return NO_LIVE_PEER;
	}

}
