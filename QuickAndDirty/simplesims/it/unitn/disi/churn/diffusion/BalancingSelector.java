package it.unitn.disi.churn.diffusion;

import java.util.BitSet;
import java.util.Random;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;

public class BalancingSelector implements IPeerSelector {

	private TIntArrayList fNeighbors = new TIntArrayList(1000);

	protected final Random fRandom;

	private final double fK;

	private final int[] fDegrees;

	public BalancingSelector(Random random, int[] degrees, double bandwidth) {
		fRandom = random;
		fK = bandwidth;
		fDegrees = degrees;
	}

	@Override
	public int selectPeer(int selecting, IndexedNeighborGraph neighbors,
			BitSet forbidden, INetwork net) {
		fNeighbors.resetQuick();

		int degree = neighbors.degree(selecting);
		for (int i = 0; i < degree; i++) {
			int neighbor = neighbors.getNeighbor(selecting, i);
			if (canSelect(forbidden, net.process(neighbor))) {
				fNeighbors.add(neighbor);
			}
		}

		if (fNeighbors.size() == 0) {
			return IPeerSelector.NO_LIVE_PEER;
		}

		int candidate = fNeighbors.get(fRandom.nextInt(fNeighbors.size()));
		if (trial(fNeighbors.size(), fDegrees[candidate])) {
			return candidate;
		}

		return IPeerSelector.NO_PEER;
	}

	private boolean trial(double delta_u, double delta_v) {
		return fRandom.nextDouble() <= Math.min(1.0, fK * (delta_u / delta_v));
	}

	public boolean canSelect(BitSet forbidden, IProcess neighbor) {
		return neighbor.isUp() && !forbidden.get(neighbor.id());
	}

}
