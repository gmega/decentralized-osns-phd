package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.List;

import peersim.core.Node;

/**
 * A merge observer is interested in knowing what goes on during an
 * {@link CompactEventStorage#merge(CompactEventStorage, IMergeObserver)}
 * operation.
 * 
 * The use of observers is more memory-efficient than recording the results of
 * the whole operation.
 * 
 * @author giuliano
 */
public interface IMergeObserver extends IEventObserver {

	public void sendDigest(Node sender, Node receiver, Node owner,
			List<Integer> holes);

	/**
	 * Null merge observer.
	 */
	public static final IMergeObserver NULL = new IMergeObserver() {
		@Override
		public void sendDigest(Node sender, Node receiver, Node owner,
				List<Integer> holes) {
		}

		@Override
		public void localDelivered(IGossipMessage message) {
		}

		@Override
		public void delivered(SNNode sender, SNNode receiver,
				IGossipMessage message, boolean duplicate) {
		}
	};
}
