package it.unitn.disi.simulator.measure;

/**
 * An {@link INamedValueObserver} allows clients to observe values that are
 * identified by strings. This allows more flexibility in observing different
 * metrics.
 * 
 * @author giuliano
 */
public interface INamedValueObserver {

	public void observe(String metric, int node, double value);

}
