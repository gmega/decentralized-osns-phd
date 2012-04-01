package it.unitn.disi.churn.connectivity;

public class SimulationResults {
	public final int source;
	public final double[] bruteForce;
	public final double[] cloud;

	public SimulationResults(int source, double[] bruteForce, double[] cloud) {
		this.source = source;
		this.bruteForce = bruteForce;
		this.cloud = cloud;
	}
}
