package it.unitn.disi.simulator.measure;

public class SumAccumulation implements IMetricAccumulator<Double> {
	
	private static final long serialVersionUID = 1L;

	private Object fId;
	
	private double [] fMetrics;
	
	public SumAccumulation(Object id, int size) {
		fMetrics = new double[size];
		fId = id;
	}

	@Override
	public Object id() {
		return fId;
	}

	@Override
	public Double getMetric(int i) {
		return fMetrics[i];
	}

	@Override
	public void add(INodeMetric<Double> metric) {
		for (int i = 0; i < fMetrics.length; i++) {
			fMetrics[i] += metric.getMetric(i);
		}
	}

	@Override
	public boolean isPreciseEnough() {
		return false;
	}

}
