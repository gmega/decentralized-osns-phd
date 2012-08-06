package it.unitn.disi.distsim.control;

public interface ManagedService extends ServiceMBean {
	public void setControl(SimulationControl parent);
}
