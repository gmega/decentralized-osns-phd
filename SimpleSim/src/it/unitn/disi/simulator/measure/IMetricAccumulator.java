package it.unitn.disi.simulator.measure;

import java.io.Serializable;

public interface IMetricAccumulator<T> extends INodeMetric<T>, Serializable {

	public abstract void add(INodeMetric<T> metric);
	
	public boolean isPreciseEnough();

}