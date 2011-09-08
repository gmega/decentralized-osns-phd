package it.unitn.disi.unitsim.ed;

import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.IUnitExperiment;

public interface IEDUnitExperiment extends IUnitExperiment {
	public void addObserver(IExperimentObserver observer);
}
