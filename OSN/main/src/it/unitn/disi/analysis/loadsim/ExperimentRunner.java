package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.utils.collections.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;

public class ExperimentRunner implements Callable<Pair<Integer, Collection<? extends MessageStatistics>>>{

	private final LoadSimulator fParent;
	
	private final IScheduler fSchedule;
	
	private final HashMap<Integer, InternalMessageStatistics> fStatistics;
	
	private final Set<Integer> fActive = new HashSet<Integer>();
	
	private final int fRoot;
	
	private LinkedList<UnitExperiment> fQueue = new LinkedList<UnitExperiment>();
	
	public ExperimentRunner(int root, IScheduler schedule, LoadSimulator parent) {
		fSchedule = schedule;
		fParent = parent;
		fStatistics = new HashMap<Integer, InternalMessageStatistics>();
		fRoot = root;
	}
	
	@Override
	public Pair<Integer, Collection<? extends MessageStatistics>> call() throws Exception {
		int round = 0;
		
		while (!fSchedule.isOver()) {
			startExperiments(round);
			runExperiments(round);
			commitResults(round);
		}
		
		return new Pair<Integer, Collection<? extends MessageStatistics>>(fRoot, fStatistics.values());
	}

	private void runExperiments(int round) {
		Iterator <UnitExperiment> it = fQueue.iterator();
		while (it.hasNext()) {
			UnitExperiment experiment = it.next();
			if (experiment.duration() == round) {			
				it.remove();
			}
			
			for (Integer nodeId : experiment.participants()) {
				InternalMessageStatistics stats = getStatistics(nodeId, experiment.degree());
				stats.roundReceived += experiment.messagesReceived(nodeId, round);
				stats.roundSent += experiment.messagesSent(nodeId, round);
				fActive.add(nodeId);
			}
		}
	}
	
	private void commitResults(int round) {
		StringBuffer buff = new StringBuffer();
		
		for (Integer nodeId : fActive) {
			InternalMessageStatistics stats = getStatistics(nodeId, -1);
			
			stats.sendBandwidth.add(stats.roundSent);
			stats.receiveBandwidth.add(stats.roundReceived);
			
			stats.roundSent = 0;
			stats.roundReceived = 0;
			
			// Prints out both values in case we want distributions later.
			buff.append("I ");
			stats.append(buff);
			buff.append(" ");
			buff.append(round);
			buff.append("\n");
		}
		
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
		fQueue.addAll(fSchedule.atTime(round));
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

