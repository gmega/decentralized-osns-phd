package it.unitn.disi.churn.diffusion;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;

import java.util.BitSet;
import java.util.Random;

public class BalancingSelector implements IPeerSelector {

	private TIntArrayList fNeighbors = new TIntArrayList(1000);

	protected final Random fRandom;

	private final double[] fInbound;

	private final double fOutbound;

	private final int[] fDegrees;

	public static BalancingSelector[] degreeApproximationSelectors(
			Random random, int[] egodegrees, int[] socialdegrees,
			double[] outbound, double[] inbound) {

		double[] actualInbound = new double[inbound.length];
		System.arraycopy(inbound, 0, actualInbound, 0, inbound.length);

		for (int i = 0; i < actualInbound.length; i++) {
			actualInbound[i] = inbound[i] / socialdegrees[i];
		}

		BalancingSelector[] selectors = new BalancingSelector[egodegrees.length];
		for (int i = 0; i < actualInbound.length; i++) {
			selectors[i] = new BalancingSelector(random, egodegrees,
					actualInbound, outbound[i] / socialdegrees[i]);
		}

		return selectors;
	}

	public BalancingSelector(Random random, int[] degrees, double[] inbound,
			double outbound) {
		fRandom = random;
		fDegrees = degrees;

		// Inbound bandwidth can be as large as we want. The limiting factor
		// is the outbound bandwidth, which cannot be larger than one as our
		// protocols send one message per round.
		fInbound = inbound;
		fOutbound = Math.min(1, outbound);
	}

	@Override
	public int selectPeer(int selecting, IndexedNeighborGraph neighbors,
			BitSet forbidden, INetwork net) {

		/**
		 * We first check whether there are available neighbors or not, so as to
		 * cause NO_LIVE_PEER or NO_PEER to take precedence over SKIP_ROUND.
		 * Failing to do so would effectively preclude the client protocol from
		 * going into sleep, rendering the simulations extremely slow.
		 */

		// Constructs filtered neighbor list.
		boolean down = false;
		fNeighbors.resetQuick();
		int degree = neighbors.degree(selecting);
		for (int i = 0; i < degree; i++) {
			int neighbor = neighbors.getNeighbor(selecting, i);
			IProcess neighborProcess = net.process(neighbor);
			if (canSelect(forbidden, neighborProcess)) {
				if (neighborProcess.isUp()) {
					fNeighbors.add(neighbor);
				} else {
					// At least one eligible neighbor can't be selected
					// because it's currently offline.
					down = true;
				}
			}
		}

		// No eligible neighbor.
		if (fNeighbors.size() == 0) {
			return down ? IPeerSelector.NO_LIVE_PEER : IPeerSelector.NO_PEER;
		}

		// Skips round to keep outbound bandwidth targets.
		if (!hasOutboundBandwidth()) {
			return IPeerSelector.SKIP_ROUND;
		}

		// Reweighted random selection.
		int candidate = fNeighbors.get(fRandom.nextInt(fNeighbors.size()));
		if (fRandom.nextDouble() < reweightFactor(fNeighbors.size(), candidate)) {
			return candidate;
		}

		// We throttle back on this round.
		return IPeerSelector.SKIP_ROUND;
	}

	private boolean hasOutboundBandwidth() {
		return fRandom.nextDouble() < fOutbound;
	}

	private double reweightFactor(int delta_u, int w) {
		return (fInbound[w] * delta_u) / (fOutbound * fDegrees[w]);
	}

	public boolean canSelect(BitSet forbidden, IProcess neighbor) {
		return !forbidden.get(neighbor.id());
	}

}
