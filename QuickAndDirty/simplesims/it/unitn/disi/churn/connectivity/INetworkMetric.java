package it.unitn.disi.churn.connectivity;

/**
 * An {@link INetworkMetric} is a named double array. It contains one double
 * value for each node in the network, representing some simulation metric that
 * can be identified by its {@link #id()}.
 * 
 * @author giuliano
 */
public interface INetworkMetric {
	/**
	 * @return the metric's ID.
	 */
	public Object id();

	/**
	 * @return the metric's value for node <code>i</code>
	 */
	public double getMetric(int i);
}
