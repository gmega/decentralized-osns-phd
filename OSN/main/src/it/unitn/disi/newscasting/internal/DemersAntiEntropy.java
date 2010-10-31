package it.unitn.disi.newscasting.internal;

import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.core.Linkable;
import peersim.core.Node;

public class DemersAntiEntropy implements IContentExchangeStrategy, Cloneable {
	
	private final int fProtocolId;
	
	private final int fSnLinkableId;
	
	public DemersAntiEntropy(int protocolId, int snLinkableId) {
		fProtocolId = protocolId;
		fSnLinkableId = snLinkableId;
	}

	public boolean doExchange(SNNode source, SNNode target) {
		// Selected node was null. Returns.
		if (target == null) {
			return false;
		}

		SocialNewscastingService application = (SocialNewscastingService) source.getProtocol(fProtocolId);
		SocialNewscastingService peerApplication = (SocialNewscastingService) target.getProtocol(fProtocolId);
		
		Linkable ourSn = (Linkable) source.getProtocol(fSnLinkableId);
		Linkable peerSn = (Linkable) source.getProtocol(fSnLinkableId);

		// Anti-entropy means merging the event histories.
		IMergeObserver observer = application.internalObserver();
		IMergeObserver peerObserver = peerApplication.internalObserver();
		
		CompactEventStorage storage = (CompactEventStorage) application.storage();
		CompactEventStorage peerStorage = (CompactEventStorage) peerApplication.storage();
		
		// Merges the peer's store into ours.
		storage.merge(target, source, peerStorage, observer, ourSn);
		peerStorage.merge(source, target, storage, peerObserver, peerSn);
		
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
}
