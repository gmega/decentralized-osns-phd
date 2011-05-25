package it.unitn.disi.analysis.loadsim;

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
public class ExperimentRunner implements Callable<TaskResult> {

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
	 * {@link IMessageSizeGenerator} used to assign size to the messages.
	 */
	private final IMessageSizeGenerator fGenerator;

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
	private final LinkedList<ScheduleEntry> fQueue = new LinkedList<ScheduleEntry>();
	
	// ----------------------------------------------------------------------

	public ExperimentRunner(UnitExperiment root, IScheduler schedule,
			ILoadSim parent, IMessageSizeGenerator generator,
			boolean printRounds) {
		fSchedule = schedule;
		fParent = parent;
		fStatistics = new HashMap<Integer, InternalMessageStatistics>();
		fRoot = root;
		fPrintRounds = printRounds;
		fGenerator = generator;
	}

	// ----------------------------------------------------------------------
	// Callable interface.
	// ----------------------------------------------------------------------

	@Override
	public TaskResult call() throws Exception {
		bootstrap();
		int round;
		for (round = 0; !fSchedule.isOver(); round++) {
			scheduleExperiments(round);
			runExperiments(round);
			commitResults(round);
		}

		return new TaskResult(round, fRoot, fStatistics.values());
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
		Iterator<ScheduleEntry> it = fQueue.iterator();
		while (it.hasNext()) {
			ScheduleEntry e = it.next();

			UnitExperiment experiment = e.experiment;
			int startTime = e.startRound;

			for (Integer nodeId : experiment.participants()) {
				// We only simulate neighborhood intersections.
				if (!fNeighborhood.contains(nodeId)) {
					continue;
				}
				// Adds the traffic from the neighboring unit experiments.
				InternalMessageStatistics stats = get(nodeId);
				stats.roundReceived += e.messageSize
						* experiment
								.messagesReceived(nodeId, round - startTime);
				stats.roundSent += e.messageSize
						* experiment.messagesSent(nodeId, round - startTime);
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
			fQueue.add(new ScheduleEntry(experiment, fGenerator.nextSize(
					experiment.id(), fParent.getGraph()), round));
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

class ScheduleEntry {

	public final UnitExperiment experiment;
	public final int messageSize;
	public final int startRound;

	public ScheduleEntry(UnitExperiment experiment, int messageSize,
			int startRound) {
		this.experiment = experiment;
		this.messageSize = messageSize;
		this.startRound = startRound;
	}
}
