package it.unitn.disi.simulator.core;

/**
 * Interface providing access to the underlying set of processes that take part
 * in the system.
 * 
 * @author giuliano
 */
public interface INetwork {

	/**
	 * @return the number of processes in the system (on-line or not).
	 */
	public int size();

	/**
	 * @return the i-th process in the network.
	 */
	public IProcess process(int i);

	/**
	 * @return the number of live nodes in the network.
	 */
	public int live();

	/**
	 * @return a versioning number. Whenever something in the network changes,
	 *         the versioning number should change as well. Version numbers are
	 *         arbitrary. The following changes should cause the versioning
	 *         number to change as well:
	 *         <ol>
	 *         <li>processes joining or leaving the network;</li>
	 *         <li>processes changing up/down state.</li>
	 *         </ol>
	 */
	public double version();

}