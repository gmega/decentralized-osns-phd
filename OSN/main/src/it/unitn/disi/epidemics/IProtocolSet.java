package it.unitn.disi.epidemics;

import it.unitn.disi.utils.IReference;

import java.util.Collection;

import peersim.core.Node;

public interface IProtocolSet {

	/**
	 * @return the PeerSim {@link Node} instance bound to this
	 *         {@link IProtocolSet}.
	 */

	public Node node();
	
	/**
	 * @return a set with the concrete types of all of the configured
	 *         {@link IContentExchangeStrategy}s.
	 */
	public Collection<Class<? extends IContentExchangeStrategy>> strategies();

	/**
	 * @return the actual instance of the {@link IContentExchangeStrategy} being
	 *         ran in under this {@link IProtocolSet}.
	 */
	public <T extends IContentExchangeStrategy> T getStrategy(Class<T> strategy);

	/**
	 * Returns the {@link IPeerSelector} instance associated with a given
	 * {@link IContentExchangeStrategy}, or <code>null</code> if the strategy is
	 * not in {@link #strategies()}.
	 * 
	 * @param strategy
	 *            a configured strategy.
	 */
	public IReference<IPeerSelector> getSelector(
			Class<? extends IContentExchangeStrategy> strategy);

	/**
	 * Returns the {@link ISelectionFilter} instance associated with a given
	 * {@link IContentExchangeStrategy}, or <code>null</code> if the strategy is
	 * not in {@link #strategies()}.
	 * 
	 * @param strategy
	 */
	public IReference<ISelectionFilter> getFilter(
			Class<? extends IContentExchangeStrategy> strategy);
}
