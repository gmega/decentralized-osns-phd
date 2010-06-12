package it.unitn.disi.application;

import it.unitn.disi.application.interfaces.IContentExchangeStrategy;
import peersim.core.Linkable;
import peersim.core.Node;

public class DemersAntiEntropy implements IContentExchangeStrategy, Cloneable {
	
	private final int fProtocolId;
	
	private final int fSnLinkableId;
	
	public DemersAntiEntropy(int protocolId, int snLinkableId) {
		fProtocolId = protocolId;
		fSnLinkableId = snLinkableId;
	}

	public boolean doExchange(Node source, Node target) {
		// Selected node was null. Returns.
		if (target == null) {
			return false;
		}

		NewscastApplication application = (NewscastApplication) source.getProtocol(fProtocolId);
		NewscastApplication peerApplication = (NewscastApplication) target.getProtocol(fProtocolId);
		
		Linkable ourSn = (Linkable) source.getProtocol(fSnLinkableId);
		Linkable peerSn = (Linkable) source.getProtocol(fSnLinkableId);

		// Anti-entropy means merging the event histories.
		IMergeObserver observer = (IMergeObserver) application.getAdapter(IMergeObserver.class, DemersAntiEntropy.class);
		IMergeObserver peerObserver = (IMergeObserver) peerApplication.getAdapter(IMergeObserver.class, DemersAntiEntropy.class);
		
		EventStorage storage = (EventStorage) application.getStorage();
		EventStorage peerStorage = (EventStorage) peerApplication.getStorage();
		
		// Merges the peer's store into ours.
		storage.merge(target, source, peerStorage, observer, ourSn);
		peerStorage.merge(source, target, storage, peerObserver, peerSn);
		
		return true;
	}

	public int throttling(Node source) {
		return 1;
	}
}
