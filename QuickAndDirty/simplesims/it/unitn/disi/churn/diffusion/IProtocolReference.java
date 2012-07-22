package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.INetwork;

/**
 * An {@link IProtocolReference} encapsulates specifics about how a protocol can
 * access its siblings at other nodes.
 * 
 * @author giuliano
 * 
 * @param <V>
 *            the protocol type for which this reference works.
 */
public interface IProtocolReference<V> {
	/**
	 * 
	 * @param caller
	 *            the calling protocol.
	 * @param network
	 *            an {@link INetwork} with all the nodes.
	 * @param id
	 *            the id of the node for which we wish to obtain the protocol
	 *            reference.
	 * @return a reference with the same type as the caller protocol.
	 */
	public V get(V caller, INetwork network, int id);
}
