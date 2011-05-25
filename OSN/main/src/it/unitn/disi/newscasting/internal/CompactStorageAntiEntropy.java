package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.List;

import peersim.core.Linkable;
import peersim.core.Node;

/**
 * "Protocol" shell which performs anti-entropy by leveraging
 * {@link CompactEventStorage}'s efficient merging capabilities.
 * 
 * @author giuliano
 */
public class CompactStorageAntiEntropy implements IContentExchangeStrategy,
		Cloneable {

	private MergeObserverImpl fObserver;

	private final int fProtocolId;

	private final int fSnLinkableId;

	public CompactStorageAntiEntropy(int protocolId, int snLinkableId) {
		fProtocolId = protocolId;
		fSnLinkableId = snLinkableId;
	}

	public boolean doExchange(SNNode source, SNNode target) {
		// Selected node was null. Returns.
		if (target == null) {
			return false;
		}

		SocialNewscastingService application = (SocialNewscastingService) source
				.getProtocol(fProtocolId);
		SocialNewscastingService peerApplication = (SocialNewscastingService) target
				.getProtocol(fProtocolId);

		Linkable ourSn = (Linkable) source.getProtocol(fSnLinkableId);
		Linkable peerSn = (Linkable) source.getProtocol(fSnLinkableId);

		CompactEventStorage storage = (CompactEventStorage) application
				.storage();
		CompactEventStorage peerStorage = (CompactEventStorage) peerApplication
				.storage();

		// Merges the peer's store into ours.
		fObserver.set(source);
		storage.merge(target, source, peerStorage, fObserver, ourSn);
		fObserver.set(target);
		peerStorage.merge(source, target, storage, fObserver, peerSn);

		return true;
	}

	public int throttling(SNNode source) {
		return 1;
	}

	@Override
	public ActivityStatus status() {
		return ActivityStatus.PERPETUAL;
	}

	@Override
	public void clear(Node source) {
		// No cache to clear.
	}

	static class MergeObserverImpl implements IMergeObserver {

		private final int fApplicationId;

		private IApplicationInterface fApplication;

		public MergeObserverImpl(int applicationId) {
			fApplicationId = applicationId;
		}

		public void set(Node node) {
			fApplication = (IApplicationInterface) node
					.getProtocol(fApplicationId);
		}

		@Override
		public void localDelivered(IGossipMessage message) {
			// This is actually never called.
			throw new IllegalStateException(
					"This shoudln't be called during merges.");
		}

		@Override
		public void delivered(SNNode sender, SNNode receiver,
				IGossipMessage message, boolean duplicate) {
			fApplication.deliver(sender, receiver, message, null);
		}

		@Override
		public void sendDigest(Node sender, Node receiver, Node owner,
				List<Integer> holes) {
			// TODO record bytes sent/received.
		}
	}
}
