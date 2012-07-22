package it.unitn.disi.simulator.churnmodel;

import it.unitn.disi.simulator.core.IProcess;

public interface IProcessFactory {
	public IProcess [] createProcesses(int n);
}
