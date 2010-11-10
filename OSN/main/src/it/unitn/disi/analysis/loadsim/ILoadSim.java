package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.graph.IndexedNeighborGraph;
import peersim.graph.Graph;

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
	 * @return whether data about a given participant of a unit experiment
	 *         should be printed or not. This can be used by other participants
	 *         to, say, discard data regarding unimportant participants.
	 * 
	 * @param experimentId
	 *            the id of a {@link UnitExperiment}.
	 * @param participantId
	 *            the id of the participant node.
	 */
	public boolean shouldPrintData(int experimentId, int participantId);

	/**
	 * @param data
	 *            prints something to the output, ensuring mutually exclusive
	 *            access to the underlying stream (so that the output doesn't
	 *            come out garbled due to concurrency).
	 */
	public abstract void synchronizedPrint(String data);

}