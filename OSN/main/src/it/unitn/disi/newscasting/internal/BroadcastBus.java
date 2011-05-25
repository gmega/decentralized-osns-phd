package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.peersim.SNNode;

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

	public void removeSubscriber(IEventObserver observer) {
		fDelegates.remove(observer);
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

	public void localDelivered(IGossipMessage message) {
		for (IEventObserver observer : fDelegates) {
			if (fCurrentBroadcaster != observer) {
				observer.localDelivered(message);
			}
		}
		fCurrentBroadcaster = null;
	}

	public void delivered(SNNode sender, SNNode receiver,
			IGossipMessage message, boolean duplicate) {
		for (IEventObserver observer : fDelegates) {
			if (fCurrentBroadcaster != observer) {
				observer.delivered(sender, receiver, message, duplicate);
			}
		}
		fCurrentBroadcaster = null;
	}
}
