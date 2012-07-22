package it.unitn.disi.simulator.measure;

public interface IMetricAccumulator<T> extends INodeMetric<T> {

	public abstract void add(INodeMetric<T> metric);

}