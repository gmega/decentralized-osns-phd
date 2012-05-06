package it.unitn.disi.churn.simulator;


import java.io.PrintStream;

import peersim.util.IncrementalStats;

public class IncrementalStatsAdapter implements IValueObserver {
	
	private final IncrementalStats fStats;
	
	public IncrementalStatsAdapter(IncrementalStats stats) {
		fStats = stats;
	}

	@Override
	public void observe(double value) {
		fStats.add(value);
	}

	public IncrementalStats getStats() {
		return fStats;
	}

	@Override
	public void print(PrintStream out) {
		out.println(fStats.toString());
	}
	
}
