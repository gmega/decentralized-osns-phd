package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.utils.collections.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * {@link ExperimentRunner} can answer what would have happened to the load in
 * the nodes of a given neighborhood if tweets had been scheduled according to a
 * given {@link IScheduler}.
 * 
 * @author giuliano
 */
public class ExperimentRunner implements
		Callable<Pair<Integer, Collection<? extends MessageStatistics>>> {

	/**
	 * The {@link IScheduler} defining how to schedule the experiments.
	 */
	private final IScheduler fSchedule;

	/**
	 * The {@link UnitExperiment} for the node at the center of this
	 * neighborhood.
	 */
	private final UnitExperiment fRoot;

	/**
	 * The parent {@link ILoadSim} which spawned this {@link ExperimentRunner}.
	 */
	private final ILoadSim fParent;

	/**
	 * Whether or not to print load information at each round.
	 */
	private final boolean fPrintRounds;

	/**
	 * {@link Set} containing the node ids for which load statistics are to be
	 * tracked.
	 */
	private final Set<Integer> fNeighborhood = new HashSet<Integer>();

	/**
	 * Load statistics.
	 */
	private final HashMap<Integer, InternalMessageStatistics> fStatistics;

	/**
	 * Experiment queue.
	 */
	private LinkedList<Pair<Integer, UnitExperiment>> fQueue = new LinkedList<Pair<Integer, UnitExperiment>>();

	// ----------------------------------------------------------------------
	
	public ExperimentRunner(UnitExperiment root, IScheduler schedule,
			ILoadSim parent, boolean printRounds) {
		fSchedule = schedule;
		fParent = parent;
		fStatistics = new HashMap<Integer, InternalMessageStatistics>();
		fRoot = root;
		fPrintRounds = printRounds;
	}
	
	// ----------------------------------------------------------------------
	// Callable interface.
	// ----------------------------------------------------------------------

	@Override
	public Pair<Integer, Collection<? extends MessageStatistics>> call()
			throws Exception {
		bootstrap();
		for (int round = 0; !fSchedule.isOver(); round++) {
			scheduleExperiments(round);
			runExperiments(round);
			commitResults(round);
		}

		return new Pair<Integer, Collection<? extends MessageStatistics>>(
				fRoot.id(), fStatistics.values());
	}
	
	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private void bootstrap() {
		for (Integer neighbor : fRoot.participants()) {
			if (fParent.shouldPrintData(fRoot.id(), neighbor)) {
				fNeighborhood.add(neighbor);
				create(neighbor, fParent.unitExperiment(neighbor).degree());
			}
		}
	}
	
	// ----------------------------------------------------------------------

	private void runExperiments(int round) {
		Iterator<Pair<Integer, UnitExperiment>> it = fQueue.iterator();
		while (it.hasNext()) {
			Pair<Integer, UnitExperiment> entry = it.next();
			int startTime = entry.a;
			UnitExperiment experiment = entry.b;

			for (Integer nodeId : experiment.participants()) {
				// We only simulate neighborhood intersections.
				if (!fNeighborhood.contains(nodeId)) {
					continue;
				}
				// Adds the traffic from the neighboring unit experiments.
				InternalMessageStatistics stats = get(nodeId);
				stats.roundReceived += experiment.messagesReceived(nodeId,
						round - startTime);
				stats.roundSent += experiment.messagesSent(nodeId, round
						- startTime);
			}

			if ((experiment.duration() + startTime - 1) == round) {
				it.remove();
				fSchedule.experimentDone(experiment);
			}
		}
	}
	
	// ----------------------------------------------------------------------

	private void commitResults(int round) {
		StringBuffer buff = fPrintRounds ? new StringBuffer() : null;

		for (Integer nodeId : fNeighborhood) {
			InternalMessageStatistics stats = get(nodeId);

			stats.sendBandwidth.add(stats.roundSent);
			stats.receiveBandwidth.add(stats.roundReceived);

			stats.sent += stats.roundSent;
			stats.received += stats.roundReceived;

			// Prints out both values in case we want distributions later.
			if (fPrintRounds) {
				buff.append("I ");
				stats.append(buff);
				buff.append(" ");
				buff.append(round);
				buff.append("\n");
			}

			// Resets the counters.
			stats.roundSent = 0;
			stats.roundReceived = 0;
		}

		if (fPrintRounds) {
			buff.delete(buff.length() - 2, buff.length());
			fParent.synchronizedPrint(buff.toString());
		}
	}
	
	// ----------------------------------------------------------------------

	private void create(int node, int degree) {
		if (fStatistics.containsKey(node)) {
			throw new IllegalStateException("Duplicate node id " + node + ".");
		}
		InternalMessageStatistics stats = new InternalMessageStatistics(node,
				degree);
		fStatistics.put(node, stats);
	}
	
	// ----------------------------------------------------------------------

	private InternalMessageStatistics get(int nodeId) {
		InternalMessageStatistics stats = fStatistics.get(nodeId);
		if (stats == null) {
			throw new IllegalStateException("Missing statistics for " + nodeId
					+ ".");
		}
		return stats;
	}
	
	// ----------------------------------------------------------------------

	private void scheduleExperiments(int round) {
		for (UnitExperiment experiment : fSchedule.atTime(round)) {
			fQueue.add(new Pair<Integer, UnitExperiment>(round, experiment));
		}
	}
	
	// ----------------------------------------------------------------------
}

class InternalMessageStatistics extends MessageStatistics {

	public int roundSent;
	public int roundReceived;

	public InternalMessageStatistics(int id, int degree) {
		super(id, degree);
	}

	public void append(StringBuffer buffer) {
		buffer.append(id);
		buffer.append(" ");
		buffer.append(degree);
		buffer.append(" ");
		buffer.append(roundSent);
		buffer.append(" ");
		buffer.append(roundReceived);
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		append(buffer);
		return buffer.toString();
	}
}
