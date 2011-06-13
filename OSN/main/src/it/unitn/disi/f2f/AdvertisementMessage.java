package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.BaseGossipMessage;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.utils.peersim.BitSetNeighborhood;

import java.util.Iterator;

import peersim.core.Linkable;
import peersim.core.Node;

public class AdvertisementMessage extends BaseGossipMessage implements
		Iterable<Node> {

	private boolean fForwarded;

	private BitSetNeighborhood fNeighborhood;

	private JoinTracker fTracker;

	public AdvertisementMessage(Node originator, int sequence, int linkable,
			JoinTracker tracker, IMessageVisibility visibility) {
		super(originator, sequence, visibility);
		fNeighborhood = new BitSetNeighborhood(
				(Linkable) originator.getProtocol(linkable));
		fTracker = tracker;
	}

	// ------------------------------------------------------------------------
	// Iterable interface.
	// ------------------------------------------------------------------------

	public Iterator<Node> iterator() {
		fNeighborhood.reset();
		return fNeighborhood;
	}

	// ------------------------------------------------------------------------
	// IGossipMessage interface.
	// ------------------------------------------------------------------------

	@Override
	public void forwarded(Node from, Node to) {
		fForwarded = true;
	}

	@Override
	public void dropped(Node at) {
		fTracker.dropped(this, at);
	}

	// ------------------------------------------------------------------------
	// Iterator interface.
	// ------------------------------------------------------------------------

	@Override
	public boolean canFlyweight() {
		return false;
	}

	@Override
	public Object clone() {
		AdvertisementMessage msg = (AdvertisementMessage) super.clone();
		msg.fNeighborhood = new BitSetNeighborhood(fNeighborhood);
		msg.fForwarded = false;
		msg.fTracker = this.fTracker;
		fTracker.copied();
		return msg;
	}

	public void reset() {
		fNeighborhood.reset();
	}

	@Override
	public int sizeOf() {
		return super.sizeOf() + (fNeighborhood.linkable().degree() * SNID_SIZE)
				+ (fNeighborhood.degree() * IPV4_SIZE);
	}

	// ------------------------------------------------------------------------
	// Other methods.
	// ------------------------------------------------------------------------

	/**
	 * Adds a node descriptor into this message.
	 * 
	 * @return <code>true</code> if the node was a part of the list of
	 *         advertised ids, or <code>false</code> otherwise.
	 */
	public boolean add(Node node) {
		return fNeighborhood.addNeighbor(node, true);
	}

	public int seen() {
		return fNeighborhood.degree();
	}

	public boolean wasForwarded() {
		return fForwarded;
	}

	public BitSetNeighborhood neighborhood() {
		return fNeighborhood;
	}
}
