package it.unitn.disi.simulator.measure;

import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import peersim.util.IncrementalStats;

public class IncrementalStatsAdapter implements IValueObserver {

	private final IReference<ISimulationEngine> fEngine;
	
	private final IncrementalStats fStats;

	private final AvgEvaluator fTester;

	private boolean fArrestCalled;

	public IncrementalStatsAdapter(IncrementalStats stats) {
		this(stats, null, null);
	}

	public IncrementalStatsAdapter(IncrementalStats stats, AvgEvaluator tester,
			IReference<ISimulationEngine> engine) {
		fStats = stats;
		fTester = tester;
		fEngine = engine;
	}

	@Override
	public synchronized void observe(double value) {
		fStats.add(value);
		if (fTester != null && fTester.isPrecise(fStats) && !fArrestCalled) {
			fEngine.get().stop();
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
