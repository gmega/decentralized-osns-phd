package it.unitn.disi.unitsim;

import it.unitn.disi.statistics.EWMAStats;
import it.unitn.disi.unitsim.cd.ICDExperimentObserver;
import it.unitn.disi.unitsim.cd.ICDUnitExperiment;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.tabular.ITableWriter;
import peersim.util.IncrementalStats;

@StructuredLog(key="PERF", fields={"completed", "experiment_avg", "cycle_avg", "cycles_per_experiment", "time_remaining"})
public class TimeTracker implements ICDExperimentObserver {

	private final long fExperiments;

	private long fEllapsedExperiments;

	private long fEllapsedCycles;

	private long fCycleStart;

	private long fExpStart;

	private IncrementalStats fCyclesPerExperiment = new IncrementalStats();

	private EWMAStats fExperimentTime;

	private EWMAStats fCycleTime;
	
	private ITableWriter fLog;

	public TimeTracker(long experiments, TabularLogManager manager) {
		fExperimentTime = new EWMAStats(200);
		fCycleTime = new EWMAStats(1000);
		fExperiments = experiments;
		fLog = manager.get(TimeTracker.class);
	}

	@Override
	public void experimentStart(IUnitExperiment experiment) {
		fExpStart = System.currentTimeMillis();
		fCycleStart = System.currentTimeMillis();
		fEllapsedCycles = 0;
	}

	@Override
	public void experimentCycled(ICDUnitExperiment experiment) {
		fCycleTime.add(System.currentTimeMillis() - fCycleStart);
		fCycleStart = System.currentTimeMillis();
		fEllapsedCycles++;
	}

	@Override
	public void experimentEnd(IUnitExperiment experiment) {
		experimentCycled((ICDUnitExperiment) experiment);
		fExperimentTime.add(System.currentTimeMillis() - fExpStart);
		fCyclesPerExperiment.add(fEllapsedCycles);
		fEllapsedExperiments++;
	}
	
	public long experimentTime() {
		return fEllapsedCycles;
	}

	public double estimateRemainingTime() {
		return (fExperiments - fEllapsedExperiments)
				* fExperimentTime.getAverage();
	}
	
	public double avgCycleTime() {
		return fCycleTime.getAverage();
	}
	
	public double avgExperimentTime() {
		return fExperimentTime.getAverage();
	}
	
	public void printStatistics() {
		fLog.set("completed", Double.toString(((double)fEllapsedExperiments)/fExperiments));
		fLog.set("experiment_avg", Double.toString(avgExperimentTime()));
		fLog.set("cycle_avg", Double.toString(avgCycleTime()));
		fLog.set("time_remaining", Double.toString(estimateRemainingTime()));
		fLog.set("cycles_per_experiment", Double.toString(fCyclesPerExperiment.getAverage()));
		fLog.emmitRow();
	}
	
}
