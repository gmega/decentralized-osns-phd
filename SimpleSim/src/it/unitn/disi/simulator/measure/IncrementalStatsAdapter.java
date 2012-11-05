package it.unitn.disi.simulator.measure;

import it.unitn.disi.simulator.core.ISimulationEngine;
import peersim.util.IncrementalStats;

public class IncrementalStatsAdapter implements IValueObserver {

	private final IncrementalStats fStats;

	private final AvgEvaluator fTester;

	private boolean fArrestCalled;

	public IncrementalStatsAdapter(IncrementalStats stats) {
		this(stats, null);
	}

	public IncrementalStatsAdapter(IncrementalStats stats, AvgEvaluator tester) {
		fStats = stats;
		fTester = tester;
	}

	@Override
	public synchronized void observe(double value, ISimulationEngine engine) {
		fStats.add(value);
		if (fTester != null && fTester.isPrecise(fStats) && !fArrestCalled) {
			engine.stop();
			fArrestCalled = true;
		}
	}

	public synchronized IncrementalStats getStats() {
		return fStats;
	}

	@Override
	public synchronized String toString() {
		return fStats.toString();
	}

}
