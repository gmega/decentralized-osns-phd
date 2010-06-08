package it.unitn.disi.application.selectors;

import it.unitn.disi.IPeerSamplingService;
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
	
	public boolean supportsFiltering() {
		return true;
	}
	
	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}
	
	public Node selectPeer(Node source, ISelectionFilter filter) {
		fCache.shuffle(source);
		// Performs the selection.
		for (int i = 0; i < fCache.size(); i++) {
			Node candidate = fCache.get(i);
			if (filter.canSelect(candidate)) {
				return filter.selected(candidate);
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
