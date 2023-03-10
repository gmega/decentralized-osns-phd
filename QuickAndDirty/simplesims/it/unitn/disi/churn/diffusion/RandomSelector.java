package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;

import java.util.BitSet;
import java.util.Random;

public class RandomSelector implements IPeerSelector {

	private TIntArrayList fNeighbors = new TIntArrayList(1000);

	protected final Random fRandom;

	public RandomSelector(Random random) {
		fRandom = random;
	}

	@Override
	public int selectPeer(int root, IndexedNeighborGraph neighbors,
			BitSet forbidden, INetwork sim) {

		fNeighbors.resetQuick();
		for (int i = 0; i < neighbors.degree(root); i++) {
			int neighbor = neighbors.getNeighbor(root, i);
			if (canSelect(forbidden, neighbor)) {
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

	protected boolean canSelect(BitSet forbidden, int neighbor) {
		return !forbidden.get(neighbor);
	}

}
