package it.unitn.disi.distsim.control;

public interface ManagedService extends ServiceMBean {

	public boolean shouldAutoStart();
	
	public void setSimulation(ISimulation parent);
}
