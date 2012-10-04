package it.unitn.disi.distsim.control;

public interface ManagedService extends ServiceMBean {

	public boolean shouldAutoStart();
	
	public void setControl(SimulationControl parent);
}
