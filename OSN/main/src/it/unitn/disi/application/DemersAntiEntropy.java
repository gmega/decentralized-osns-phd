package it.unitn.disi.application;

import it.unitn.disi.IAdaptable;
import it.unitn.disi.application.interfaces.IContentExchangeStrategy;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.PeersimUtils;
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

		IAdaptable adaptable = (IAdaptable) source.getProtocol(fProtocolId);
		IAdaptable peerAdaptable = (IAdaptable) target.getProtocol(fProtocolId);
		
		Linkable ourSn = (Linkable) PeersimUtils.getLinkable(source, fProtocolId,
				fSnLinkableId);
		Linkable peerSn = (Linkable) PeersimUtils.getLinkable(target, fProtocolId,
				fSnLinkableId);

		// Anti-entropy means merging the event histories.
		IMergeObserver observer = (IMergeObserver) adaptable.getAdapter(IMergeObserver.class, DemersAntiEntropy.class);
		IMergeObserver peerObserver = (IMergeObserver) peerAdaptable.getAdapter(IMergeObserver.class, DemersAntiEntropy.class);
		
		EventStorage storage = (EventStorage) adaptable.getAdapter(EventStorage.class, null);
		EventStorage peerStorage = (EventStorage) peerAdaptable.getAdapter(EventStorage.class, null);
		
		// Merges the peer's store into ours.
		storage.merge(target, source, peerStorage, observer, ourSn);
		peerStorage.merge(source, target, storage, peerObserver, peerSn);
		
		return true;
	}

	public int throttling() {
		return 1;
	}
}
