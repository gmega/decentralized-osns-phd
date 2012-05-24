package it.unitn.disi.churn.diffusion.graph;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.collections.Triplet;

/**
 * {@link ILiveTransformer} creates transformed versions of
 * {@link IndexedNeighborGraph} and {@link INetwork} based on node up/down state
 * described by a source {@link INetwork}.
 * 
 * @author giuliano
 */
public interface ILiveTransformer {

	/**
	 * Returned when no live peers exist.
	 */
	public static final Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> NO_LIVE_PEER = new Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph>(
			null, null, null);

	/**
	 * @param source
	 *            the source graph.
	 * 
	 * @param network
	 *            the {@link SimpleEDSim} from which peer state can be queried.
	 * 
	 * @return a {@link Triplet}, with:
	 *         <ol>
	 *         <li>an {@link IndexedNeighborGraph} which is a subgraph of the
	 *         original graph containing only live peers;</li>
	 *         <li>an {@link IDMapper} which allows ids to be mapped from the
	 *         subgraph back into the original graph;
	 *         <li>an {@link INetwork} instance which can be used to access the
	 *         processes in remapped id space.</li>
	 *         </ol>
	 *         If there are no live peers in the network, returns
	 *         {@link #NO_LIVE_PEER}.
	 */
	public Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> live(
			IndexedNeighborGraph source, INetwork network);
}
