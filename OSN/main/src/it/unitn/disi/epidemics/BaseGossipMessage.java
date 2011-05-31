package it.unitn.disi.epidemics;

import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.utils.MiscUtils;
import peersim.core.Node;

public abstract class BaseGossipMessage implements IGossipMessage, Cloneable {

	/**
	 * The node who produced the content.
	 */
	public final Node poster;

	/**
	 * The producer-relative sequence number for this content.
	 */
	public final int sequenceNumber;

	/**
	 * An {@link IMessageVisibility} instance, which computes the intended
	 * destinations for this message.
	 */
	private final IMessageVisibility fDestinations;

	/**
	 * The computed hash code.
	 */
	private final int fHashCode;

	public BaseGossipMessage() {
		poster = null;
		sequenceNumber = -1;
		fDestinations = null;
		fHashCode = super.hashCode();
	}

	public BaseGossipMessage(Node poster, int sequenceNumber,
			IMessageVisibility visibility) {
		this.poster = poster;
		this.sequenceNumber = sequenceNumber;
		this.fDestinations = visibility;
		fHashCode = computeHash();
	}

	@Override
	public Node originator() {
		return poster;
	}

	@Override
	public int sequenceNumber() {
		return sequenceNumber;
	}

	@Override
	public int destinations() {
		return fDestinations.size(this);
	}

	@Override
	public Node destination(int i) {
		return fDestinations.get(this, i);
	}

	@Override
	public boolean isDestination(Node node) {
		return fDestinations.isDestination(this, node);
	}

	@Override
	public void forwarded(Node from, Node to) {
		// By default, does nothing.
	}

	@Override
	public void dropped(Node at) {
		// By default, does nothing.
	}

	@Override
	public boolean equals(Object other) {
		boolean equals = false;
		if (other instanceof BaseGossipMessage) {
			BaseGossipMessage msg = (BaseGossipMessage) other;
			equals = msg.poster.equals(poster)
					&& msg.sequenceNumber == sequenceNumber;
		}
		return equals;
	}

	@Override
	public int hashCode() {
		return fHashCode;
	}

	@Override
	public boolean canFlyweight() {
		return true;
	}

	@Override
	public IGossipMessage cloneIfNeeded() {
		if (canFlyweight()) {
			return this;
		}
		return (IGossipMessage) clone();
	}

	@Override
	public Object clone() {
		try {
			Object clone = super.clone();
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	protected int computeHash() {
		int result = 47;
		result = 37 * result + poster.hashCode();
		result = 37 * result + sequenceNumber;
		return result;
	}
}
