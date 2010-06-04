package it.unitn.disi.application;

import it.unitn.disi.application.interfaces.IEventObserver;

import java.util.List;

import peersim.core.Node;

/**
 * A merge observer is interested in knowing what goes on during an
 * {@link EventStorage#merge(EventStorage, IMergeObserver)} operation.
 * 
 * The use of observers is more memory-efficient than recording the results of
 * the whole operation.
 * 
 * @author giuliano
 */
interface IMergeObserver extends IEventObserver {

	public void sendDigest(Node sender, Node receiver, Node owner,
			List<Integer> holes);

	/**
	 * Null merge observer.
	 */
	public static final IMergeObserver NULL = new IMergeObserver() {

		public void sendDigest(Node sender, Node receiver, Node owner,
				List<Integer> holes) { }

		public void eventDelivered(Node sender, Node receiver, Node owner,
				int start, int end) { }

		public void duplicateReceived(Node sender, Node receiver, Node owner,
				int start, int end) { }

		public void tweeted(Node owner, int sequenceNumber) { }
	};
}
