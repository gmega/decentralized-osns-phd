package it.unitn.disi.newscasting;

import it.unitn.disi.newscasting.internal.DefaultVisibility;
import peersim.core.Node;

//----------------------------------------------------------------------

/**
 * A {@link Tweet} represents an <i>immutable</i> piece of content (text,
 * picture, song, video, etc.) posted by a user to its profile page.
 */
public class Tweet {

	/**
	 * The node who produced the content.
	 */
	public final Node poster;

	/**
	 * If this is a reply tweet, then this field should contain the
	 * {@link Tweet} to which this {@link Tweet} is replying to.
	 */
	public final Tweet parent;

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

	/**
	 * Convenience constructor. Equivalent to:
	 * 
	 * <code> Tweet(originator, sequenceNumber, new DefaultVisibility(sequenceNumber), null);  </code>
	 */
	public Tweet(Node poster, int sequenceNumber) {
		this(poster, sequenceNumber, (Tweet) null);
	}

	/**
	 * Convenience constructor. Equivalent to:
	 * 
	 * <code> Tweet(originator, sequenceNumber, new DefaultVisibility(sequenceNumber), original);  </code>
	 */
	public Tweet(Node poster, int sequenceNumber, Tweet original) {
		this(poster, sequenceNumber, new DefaultVisibility(sequenceNumber),
				original);
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
	 *            to.
	 */
	public Tweet(Node poster, int sequenceNumber,
			IMessageVisibility visibility, Tweet original) {
		this.poster = poster;
		this.sequenceNumber = sequenceNumber;
		this.parent = original;
		this.fDestinations = visibility;

		fHashCode = computeHash();
	}

	/**
	 * @return the number of destinations for this {@link Tweet}.
	 */
	public int destinations() {
		return fDestinations.size(this);
	}

	/**
	 * @return the i-th destination for this {@link Tweet}.
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             if i >= {@link #destinations()}.
	 */
	public Node destination(int i) {
		return fDestinations.get(this, i);
	}

	/**
	 * @return <code>true</code> if a {@link Node} is a destination for this
	 *         {@link Tweet}, or <code>false</code> otherwise.
	 */
	public boolean isDestination(Node node) {
		return fDestinations.isDestination(this, node);
	}

	/**
	 * @return the node owning the profile to which this {@link Tweet} was
	 *         posted.
	 */
	public Node profile() {
		if (parent == null) {
			return this.poster;
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
	public int hashCode() {
		return fHashCode;
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

	private int computeHash() {
		int result = 47;
		result = 37 * result + poster.hashCode();
		result = 37 * result + sequenceNumber;
		if (parent != null) {
			result = 37 * result + parent.hashCode();
		}

		return result;
	}
}