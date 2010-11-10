package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.utils.collections.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;

public class ExperimentRunner implements
		Callable<Pair<Integer, Collection<? extends MessageStatistics>>> {

	private final ILoadSim fParent;

	private final IScheduler fSchedule;

	private final HashMap<Integer, InternalMessageStatistics> fStatistics;

	private final Set<Integer> fNeighborhood = new HashSet<Integer>();

	private final UnitExperiment fRoot;

	private LinkedList<Pair<Integer, UnitExperiment>> fQueue = new LinkedList<Pair<Integer, UnitExperiment>>();

	public ExperimentRunner(UnitExperiment root, IScheduler schedule,
			ILoadSim parent) {
		fSchedule = schedule;
		fParent = parent;
		fStatistics = new HashMap<Integer, InternalMessageStatistics>();
		fRoot = root;
	}

	@Override
	public Pair<Integer, Collection<? extends MessageStatistics>> call()
			throws Exception {
		
		for (Integer neighbor : fRoot.participants()) {
			if (fParent.shouldPrintData(fRoot.id(), neighbor)) {
				fNeighborhood.add(neighbor);
			}
		}

		for (int round = 0; !fSchedule.isOver(); round++) {
			startExperiments(round);
			runExperiments(round);
			commitResults(round);
		}

		return new Pair<Integer, Collection<? extends MessageStatistics>>(
				fRoot.id(), fStatistics.values());
	}

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
				InternalMessageStatistics stats = getStatistics(nodeId, fParent
						.unitExperiment(nodeId).degree());
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

	private void commitResults(int round) {
		StringBuffer buff = new StringBuffer();

		for (Integer nodeId : fNeighborhood) {
			InternalMessageStatistics stats = getStatistics(nodeId, -1);

			stats.sendBandwidth.add(stats.roundSent);
			stats.receiveBandwidth.add(stats.roundReceived);

			stats.sent += stats.roundSent;
			stats.received += stats.roundReceived;

			// Prints out both values in case we want distributions later.
			buff.append("I ");
			stats.append(buff);
			buff.append(" ");
			buff.append(round);
			buff.append("\n");

			// Resets the counters.
			stats.roundSent = 0;
			stats.roundReceived = 0;
		}
		
		buff.delete(buff.length() - 2, buff.length());

		fParent.synchronizedPrint(buff.toString());
	}

	private InternalMessageStatistics getStatistics(int nodeId, int degree) {
		InternalMessageStatistics stats = fStatistics.get(nodeId);
		if (stats == null) {
			if (degree < 0) {
				throw new IllegalStateException();
			}
			stats = new InternalMessageStatistics(nodeId, degree);
			fStatistics.put(nodeId, stats);
		}
		return stats;
	}

	private void startExperiments(int round) {

		for (UnitExperiment experiment : fSchedule.atTime(round)) {
			fQueue.add(new Pair<Integer, UnitExperiment>(round, experiment));
		}
	}
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
