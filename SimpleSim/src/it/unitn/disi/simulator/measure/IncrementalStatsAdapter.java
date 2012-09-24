package it.unitn.disi.simulator.measure;


import peersim.util.IncrementalStats;

public class IncrementalStatsAdapter implements IValueObserver {
	
	private final IncrementalStats fStats;
	
	public IncrementalStatsAdapter(IncrementalStats stats) {
		fStats = stats;
	}

	@Override
	public synchronized void observe(double value) {
		fStats.add(value);
	}

	public synchronized IncrementalStats getStats() {
		return fStats;
	}

	@Override
	public synchronized String toString() {
		return fStats.toString();
	}
	
}
