package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.statistics.EWMAStats;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import peersim.core.Node;
import peersim.util.IncrementalStats;

@StructuredLog(key="PERF", fields={"completed", "experiment_avg", "cycle_avg", "cycles_per_experiment", "time_remaining"})
public class TimeTracker implements IExperimentObserver {

	private final long fExperiments;

	private long fEllapsedExperiments;

	private long fEllapsedCycles;

	private long fCycleStart;

	private long fExpStart;

	private IncrementalStats fCyclesPerExperiment = new IncrementalStats();

	private EWMAStats fExperimentTime;

	private EWMAStats fCycleTime;
	
	private TableWriter fLog;

	public TimeTracker(long experiments, TabularLogManager manager) {
		fExperimentTime = new EWMAStats(200);
		fCycleTime = new EWMAStats(1000);
		fExperiments = experiments;
		fLog = manager.get(TimeTracker.class);
	}

	@Override
	public void experimentStart(Node root) {
		fExpStart = System.currentTimeMillis();
		fCycleStart = System.currentTimeMillis();
		fEllapsedCycles = 0;
	}

	@Override
	public void experimentCycled(Node root) {
		fCycleTime.add(System.currentTimeMillis() - fCycleStart);
		fCycleStart = System.currentTimeMillis();
		fEllapsedCycles++;
	}

	@Override
	public void experimentEnd(Node root) {
		experimentCycled(root);
		fExperimentTime.add(System.currentTimeMillis() - fExpStart);
		fCyclesPerExperiment.add(fEllapsedCycles);
		fEllapsedExperiments++;
	}

	public double remainingTime() {
		return (fExperiments - fEllapsedExperiments)
				* fExperimentTime.getAverage();
	}
	
	public double cycleTime() {
		return fCycleTime.getAverage();
	}
	
	public double experimentTime() {
		return fExperimentTime.getAverage();
	}
	
	public void printStatistics() {
		fLog.set("completed", Double.toString(((double)fEllapsedExperiments)/fExperiments));
		fLog.set("experiment_avg", Double.toString(experimentTime()));
		fLog.set("cycle_avg", Double.toString(cycleTime()));
		fLog.set("time_remaining", Double.toString(remainingTime()));
		fLog.set("cycles_per_experiment", Double.toString(fCyclesPerExperiment.getAverage()));
		fLog.emmitRow();
	}
	
}
