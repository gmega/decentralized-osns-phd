package it.unitn.disi;

import it.unitn.disi.protocol.selectors.ISelectionFilter;
import it.unitn.disi.utils.PermutingCache;
import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * Simple peer sampling service which performs random selection over a
 * {@link Linkable}.
 * 
 * @author giuliano
 */
public class RandomSelectorOverLinkable implements IPeerSamplingService, Protocol {

	private static final String PAR_LINKABLE = "linkable";
	
	private PermutingCache fCache;

	public RandomSelectorOverLinkable(String name) {
		this(Configuration.getPid(name + "." + PAR_LINKABLE));
	}
	
	public RandomSelectorOverLinkable(int pid) {
		fCache = new PermutingCache(pid);
	}
	
	public Node selectPeer(Node source, ISelectionFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean supportsFiltering() {
		// TODO Auto-generated method stub
		return false;
	}

	public Node selectPeer(Node source) {
		fCache.shuffle(source);
		// Performs the selection.
		for (int i = 0; i < fCache.size(); i++) {
			if (fCache.get(i).isUp()) {
				return fCache.get(i);
			}
		}

		return null;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
