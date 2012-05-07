package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.graph.IndexedNeighborGraph;

import java.util.BitSet;

public interface IPeerSelector {

	public static int NO_LIVE_PEER = -1;

	public static int NO_PEER = -2;

	/**
	 * Selects a peer.
	 * 
	 * @param selecting
	 *            the peer performing the selection.
	 * @param neighbors
	 *            the graph representing the social overlay.
	 * @param forbidden
	 *            a {@link BitSet} with the neighbor ids which should be
	 *            excluded from selection.
	 * @param sim
	 *            the parent simulator instance.
	 * @return a peer id.
	 */
	public int selectPeer(int selecting, IndexedNeighborGraph neighbors,
			BitSet forbidden, INetwork sim);

}
