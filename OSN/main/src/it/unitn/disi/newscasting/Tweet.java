package it.unitn.disi.newscasting;

import it.unitn.disi.epidemics.BaseGossipMessage;
import peersim.core.Node;

//----------------------------------------------------------------------

/**
 * A {@link Tweet} represents an <i>immutable</i> piece of content (text,
 * picture, song, video, etc.) posted by a user to its profile page.
 */
public class Tweet extends BaseGossipMessage {
	
	public static final Tweet UNKNOWN_PARENT = new Tweet();
	
	public final Tweet parent;	

	private Tweet() {
		super();
		this.parent = null;
	}

	/**
	 * Convenience constructor. Equivalent to:
	 * 
	 * <code> Tweet(originator, sequenceNumber, visibility, null); </code>
	 */
	public Tweet(Node poster, int sequenceNumber, IMessageVisibility visibility) {
		this(poster, sequenceNumber, visibility, null);
	}

	/**
	 * Constructs a new reply {@link Tweet}.
	 * 
	 * @param originator
	 *            the node posting the reply.
	 * 
	 * @param sequenceNumber
	 *            the originator-relative sequence number.
	 * 
	 * @param original
	 *            the {@link Tweet} to which this content is supposed to reply
	 *            to, or <code>null</code> if this {@link Tweet} is not a reply.
	 */
	public Tweet(Node poster, int sequenceNumber,
			IMessageVisibility visibility, Tweet original) {
		super(poster, sequenceNumber, visibility);
		this.parent = original;
	}
	
	// ------------------------------------------------------------------------
	// IGossipMessage methods.
	// ------------------------------------------------------------------------
	
	@Override
	public Tweet payload() {
		return this;
	}
	
	// ------------------------------------------------------------------------
	// Methods specific to Tweets.
	// ------------------------------------------------------------------------

	/**
	 * @return the node owning the profile to which this {@link Tweet} was
	 *         posted.
	 */
	public Node profile() {
		if (parent == null) {
			return this.originator();
		}
		return parent.profile();
	}

	@Override
	public boolean equals(Object other) {
		boolean equals = false;
		if (other instanceof Tweet) {
			Tweet evt = (Tweet) other;
			// Base test.
			equals = evt.poster.equals(poster)
					&& evt.sequenceNumber == sequenceNumber;

			if (this.parent != null) {
				equals &= (this.parent.equals(evt.parent));
			} else {
				equals &= (evt.parent == null);
			}
		}
		return equals;
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("(p:");
		buffer.append(poster.getID());
		buffer.append(", s:");
		buffer.append(sequenceNumber);
		if (parent != null) {
			buffer.append(" -> [");
			buffer.append(parent.toString());
			buffer.append("]");
		}
		buffer.append(")");

		return buffer.toString();
	}

	protected int computeHash() {
		int result = super.computeHash();
		if (parent != null) {
			result = 37 * result + parent.hashCode();
		}
		return result;
	}
}