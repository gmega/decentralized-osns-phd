package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.utils.peersim.PermutingCache;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.resolvers.PeerSimResolver;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * Simple {@link IPeerSelector} implementation which performs random selection
 * over a {@link Linkable} (ideal peer sampler).
 * 
 * @author giuliano
 */
public class RandomSelectorOverLinkable implements IPeerSelector, Protocol {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	
	private static final String PAR_LINKABLE = "linkable";

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------
	
	private PermutingCache fCache;

	// ----------------------------------------------------------------------

	public RandomSelectorOverLinkable(String name) {
		this(new PeerSimResolver(), name);
	}
	
	// ----------------------------------------------------------------------
	
	public RandomSelectorOverLinkable(IResolver resolver, String name) {
		this(resolver.getInt(name, PAR_LINKABLE));
	}
	
	// ----------------------------------------------------------------------
	
	public RandomSelectorOverLinkable(int pid) {
		fCache = new PermutingCache(pid);
	}
	
	// ----------------------------------------------------------------------
	// IPeerSelector interface.
	// ----------------------------------------------------------------------
	public boolean supportsFiltering() {
		return true;
	}
	
	// ----------------------------------------------------------------------
	
	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}
	
	// ----------------------------------------------------------------------
	
	public Node selectPeer(Node source, ISelectionFilter filter) {
		fCache.populate(source);
		fCache.shuffle();
		// Performs the selection.
		for (int i = 0; i < fCache.size(); i++) {
			Node candidate = fCache.get(i);
			if (filter.canSelect(candidate)) {
				return filter.selected(candidate);
			}
		}

		return null;
	}
	
	// ----------------------------------------------------------------------
	
	@Override
	public void clear(Node node) {
		// No cache to clear.
	}
	
	// ----------------------------------------------------------------------
	// Protocol interface.	
	// ----------------------------------------------------------------------
	
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
