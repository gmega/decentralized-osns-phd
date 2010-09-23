package it.unitn.disi.analysis.loadsim;

import java.util.List;

public interface IScheduler {
	public boolean isOver();
	
	public List<UnitExperiment> atTime(int round);
	
	public boolean experimentDone(UnitExperiment experiment);
}
