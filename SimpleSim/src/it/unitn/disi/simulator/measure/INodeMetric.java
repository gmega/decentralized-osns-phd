package it.unitn.disi.simulator.measure;

/**
 * An {@link INodeMetric} is a named array. It contains one value for each node
 * in the network, representing some simulation metric that can be identified by
 * its {@link #id()}.
 * 
 * @author giuliano
 */
public interface INodeMetric<T> {
	/**
	 * @return the metric's ID.
	 */
	public Object id();

	/**
	 * @return the metric's value for node <code>i</code>
	 */
	public T getMetric(int i);
}
