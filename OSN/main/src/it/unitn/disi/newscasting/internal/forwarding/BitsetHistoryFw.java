package it.unitn.disi.newscasting.internal.forwarding;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.collections.FastGetBitset;
import peersim.config.IResolver;
import peersim.core.Node;

/**
 * Simpler, less memory-efficient and faster implementation of histories based
 * on BitSets.
 * 
 * @author giuliano
 */
public class BitsetHistoryFw extends CachingHistoryFw<FastGetBitset> {

	public BitsetHistoryFw(int adaptableId, int socialNetworkId,
			IResolver resolver, String prefix) {
		super(adaptableId, socialNetworkId, resolver, prefix);
	}

	public BitsetHistoryFw(int adaptableId, int socialNetworkId, int chunkSize,
			int windowSize) {
		super(adaptableId, socialNetworkId, chunkSize, windowSize);
	}

	@Override
	protected void historyAdd(Object history, Node node) {
		bitSet(history).set(id(node));
	}

	@Override
	protected boolean historyContains(Object history, Node element) {
		return bitSet(history).get(id(element));
	}

	@Override
	protected void historyMerge(Object merged, Object mergee) {
		bitSet(merged).or(bitSet(mergee));
	}

	@Override
	protected Object historyClone(IGossipMessage message, Object history) {
		return cache(message, (FastGetBitset) bitSet(history).clone());
	}

	@Override
	protected Object historyCreate(IGossipMessage message) {
		return cache(message, new FastGetBitset());
	}

	private FastGetBitset bitSet(Object history) {
		return (FastGetBitset) history;
	}

	private int id(Node node) {
		return (int) node.getID();
	}

}
