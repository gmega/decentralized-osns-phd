package it.unitn.disi.newscasting.internal;

import it.unitn.disi.newscasting.Tweet;

import java.util.ArrayList;
import java.util.List;

import peersim.core.Node;

public class BroadcastBus implements IMergeObserver {

	private final ArrayList<IEventObserver> fDelegates = new ArrayList<IEventObserver>();

	private IEventObserver fCurrentBroadcaster;

	public void beginBroadcast(IEventObserver observer) {
		fCurrentBroadcaster = observer;
	}

	public void addSubscriber(IEventObserver observer) {
		fDelegates.add(observer);
	}

	public void sendDigest(Node sender, Node receiver, Node owner,
			List<Integer> holes) {
		for (IEventObserver observer : fDelegates) {
			if (observer instanceof IMergeObserver
					&& fCurrentBroadcaster != observer) {
				((IMergeObserver) observer).sendDigest(sender, receiver, owner,
						holes);
			}
		}
		fCurrentBroadcaster = null;
	}

	public void tweeted(Tweet tweet) {
		for (IEventObserver observer : fDelegates) {
			if (fCurrentBroadcaster != observer) {
				observer.tweeted(tweet);
			}
		}
		fCurrentBroadcaster = null;
	}

	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate) {
		for (IEventObserver observer : fDelegates) {
			if (fCurrentBroadcaster != observer) {
				observer.eventDelivered(sender, receiver, tweet, duplicate);
			}
		}
		fCurrentBroadcaster = null;
	}
}
