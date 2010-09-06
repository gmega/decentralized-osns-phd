package it.unitn.disi.newscasting.experiments;

import peersim.config.AutoConfig;
import peersim.core.Node;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.Tweet;


public class OneThanTheOther implements IPeerSelector {
	
	private Tweet fTweet;
	
	private IPeerSelector fFirst;
	
	private IPeerSelector fSecond;
	
	private int fN0;
	
	public void OneThanTheOther(IPeerSelector first, IPeerSelector second, int n0) { 
		fFirst = first;
		fSecond = second;
		fN0 = n0;
	}

	@Override
	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	@Override
	public Node selectPeer(Node source, ISelectionFilter filter) {
		if (fTweet == null) {
			return null;
		}
		return null;
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	@Override
	public void clear(Node source) {
		// Nothing to clear.
	}
	
	
	
}
