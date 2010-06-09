package it.unitn.disi.protocol;

import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.protocol.selectors.ISelector;
import peersim.core.Node;


/**
 * {@link PeerSelectorChain} allows peer selectors to be composed under a
 * single object. In essence, a chained selector will try one selector after the
 * other, until proper results are yielded.
 */
class PeerSelectorChain implements ISelector {

	private ISelector[] fChain;

	public PeerSelectorChain(ISelector[] chain) {
		fChain = chain;
	}

	public Node selectPeer(ISelectionFilter filter) {
		for (int i = 0; i < fChain.length; i++) {
			Node selected = fChain[i].selectPeer(filter);
			if (selected != null) {
				return selected;
			}
		}
		return null;
	}

}

