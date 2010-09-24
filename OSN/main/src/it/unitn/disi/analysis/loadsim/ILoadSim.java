package it.unitn.disi.analysis.loadsim;

import peersim.graph.Graph;
import it.unitn.disi.utils.graph.IndexedNeighborGraph;

/**
 * {@link ILoadSim} is the interface providing access to read-only, shared data
 * objects to the participants actually doing the simulation work.
 * 
 * Implementations should always be thread safe.
 * 
 * @author giuliano
 */
public interface ILoadSim {

	/**
	 * @return the {@link UnitExperiment} for a given node id.
	 */
	public abstract UnitExperiment unitExperiment(int nodeId);

	/**
	 * @return the {@link Graph} over which the unit experiments have been
	 *         realized.
	 */
	public abstract IndexedNeighborGraph getGraph();

	/**
	 * @param data
	 *            prints something to the output, ensuring mutually exclusive
	 *            access to the underlying stream (so that the output doesn't
	 *            come out garbled due to concurrency).
	 */
	public abstract void synchronizedPrint(String data);

}