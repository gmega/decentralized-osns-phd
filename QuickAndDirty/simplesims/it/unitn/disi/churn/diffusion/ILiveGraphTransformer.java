package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.collections.Pair;

/**
 * {@link ILiveGraphTransformer} creates a subgraph of an
 * {@link IndexedNeighborGraph} which contains only the vertices corresponding
 * to nodes that are alive.
 * 
 * @author giuliano
 */
public interface ILiveGraphTransformer {

	/**
	 * Returns the subgraph containing live peers.
	 * 
	 * @param source
	 *            the source graph.
	 * 
	 * @param network
	 *            the {@link SimpleEDSim} from which peer state can be queried.
	 * 
	 * @return a {@link Pair}, with an {@link AbstractIDMapper} mapping the ids
	 *         of the original graph in the "live" one, or <code>null</code> if
	 *         all peers are alive.
	 */
	public Pair<AbstractIDMapper, IndexedNeighborGraph> liveGraph(
			IndexedNeighborGraph source, INetwork network);
}
