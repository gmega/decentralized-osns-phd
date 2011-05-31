package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.BaseGossipMessage;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.utils.peersim.PeersimUtils;

import java.util.BitSet;
import java.util.Iterator;

import peersim.core.Linkable;
import peersim.core.Node;

public class AdvertisementMessage extends BaseGossipMessage implements
		Iterator<Node> {

	private final Node fOriginator;

	private final int fLinkable;

	private boolean fForwarded;

	private BitSet fSeen = new BitSet();

	private int fIndex = 0;

	private JoinTracker fTracker;

	public AdvertisementMessage(Node originator, int sequence, int linkable,
			JoinTracker tracker, IMessageVisibility visibility) {
		super(originator, sequence, visibility);
		fOriginator = originator;
		fLinkable = linkable;
		fTracker = tracker;
	}

	public boolean add(Node node) {
		int index = PeersimUtils.indexOf(node, linkable());
		if (index == -1) {
			return false;
		}
		fSeen.set(index);
		return true;
	}

	public int seen() {
		return fSeen.cardinality();
	}

	public void or(BitSet other) {
		other.or(fSeen);
	}

	private Linkable linkable() {
		return ((Linkable) fOriginator.getProtocol(fLinkable));
	}

	@Override
	public boolean hasNext() {
		return fSeen.nextSetBit(fIndex) != -1;
	}

	@Override
	public Node next() {
		fIndex = fSeen.nextSetBit(fIndex);
		return linkable().getNeighbor(fIndex);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean canFlyweight() {
		return false;
	}

	@Override
	public Object clone() {
		AdvertisementMessage msg = (AdvertisementMessage) super.clone();
		msg.fSeen = new BitSet();
		msg.fSeen.or(this.fSeen);
		msg.fForwarded = false;
		msg.fTracker = this.fTracker;
		fTracker.copied();
		return msg;
	}

	public void reset() {
		fIndex = 0;
	}

	@Override
	public void forwarded(Node from, Node to) {
		fForwarded = true;
	}

	public boolean wasForwarded() {
		return fForwarded;
	}

	@Override
	public void dropped(Node at) {
		fTracker.dropped(this, at);
	}

}
