package it.unitn.disi.simulator.measure;

public class SumAccumulation implements IMetricAccumulator<Double> {

	private double[] fMetric;

	private Object fId;

	public SumAccumulation(Object id, int length) {
		fId = id;
		fMetric = new double[length];
	}

	/* (non-Javadoc)
	 * @see it.unitn.disi.simulator.measure.IAccumulatingMetric#id()
	 */
	@Override
	public Object id() {
		return fId;
	}

	/* (non-Javadoc)
	 * @see it.unitn.disi.simulator.measure.IAccumulatingMetric#getMetric(int)
	 */
	@Override
	public Double getMetric(int i) {
		return fMetric[i];
	}

	/* (non-Javadoc)
	 * @see it.unitn.disi.simulator.measure.IAccumulatingMetric#add(it.unitn.disi.simulator.measure.INetworkMetric)
	 */
	@Override
	public void add(INodeMetric<Double> metric) {
		for (int i = 0; i < fMetric.length; i++) {
			fMetric[i] += metric.getMetric(i);
		}
	}
	
}
