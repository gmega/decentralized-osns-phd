package it.unitn.disi.application;

import peersim.core.Node;

//----------------------------------------------------------------------

/**
 * Immutable tuple which represents events.
 */
public class Tweet {
	public final Node fNode;
	public final int fSequence;
	private final int fHashCode;

	public Tweet(Node node, int sequence) {
		fNode = node;
		fSequence = sequence;
		fHashCode = computeHash();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Tweet) {
			Tweet evt = (Tweet) other;
			return evt.fNode.equals(fNode) && evt.fSequence == fSequence;
		}

		return false;
	}

	@Override
	public int hashCode() {
		return fHashCode; 
	}

	@Override
	public String toString() {
		return Integer.toString(fSequence);
	}
	
	private int computeHash() {
		int result = 47;
		result = 37*result + fNode.hashCode();
		result = 37*result + fSequence;
		return result;
	}
}