package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.simulator.measure.INodeMetric;

public class SimpleMutableMetric implements INodeMetric<Double>{
	
	private Object fId;
	
	private double [] fMetric;
	
	public SimpleMutableMetric(Object id, int size) {
		fMetric = new double[size];
		fId = id;
	}

	@Override
	public Object id() {
		return fId;
	}

	@Override
	public Double getMetric(int i) {
		return fMetric[i];
	}
	
	public void setValue(double value, int node) {
		fMetric[node] = value;
	}

}
