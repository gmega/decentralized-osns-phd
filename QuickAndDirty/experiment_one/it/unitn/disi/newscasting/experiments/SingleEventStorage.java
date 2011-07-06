package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IWritableEventStorage;

import java.util.Iterator;
import java.util.Set;

import peersim.core.Node;

public class SingleEventStorage implements IWritableEventStorage {

	private IGossipMessage fTweet;

	@Override
	public boolean add(IGossipMessage tweet) {
		if (fTweet == null) {
			fTweet = tweet;
			return true;
		}

		if (!contains(tweet)) {
			throw new IllegalStateException("Tried to add " + tweet
					+ " but had " + fTweet + ".");
		}

		return false;
	}

	@Override
	public void clear() {
		fTweet = null;
	}

	@Override
	public boolean contains(IGossipMessage tweet) {
		return tweet.equals(fTweet);
	}

	@Override
	public int distinctNodes() {
		return (fTweet == null) ? 0 : 1;
	}

	@Override
	public int elements() {
		return (fTweet == null) ? 0 : 1;
	}

	@Override
	public boolean remove(Node node, int sequence) {
		if (fTweet == null) {
			return false;
		}

		if (fTweet.originator().equals(node)
				&& fTweet.sequenceNumber() == sequence) {
			fTweet = null;
			return true;
		}

		return false;
	}

	@Override
	public Set<Node> nodes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<IGossipMessage> tweetsFor(Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object clone() {
		try {
			return (SingleEventStorage) super.clone();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public IGossipMessage retrieve(Node originator, int sequence) {
		if (fTweet != null && fTweet.originator().equals(originator)
				&& fTweet.sequenceNumber() == sequence) {
			return fTweet;
		}
		return null;
	}

}
