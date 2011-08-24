package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.IPushPeerSelector;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.PermutingCache;
import peersim.config.IResolver;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * Simple {@link IPeerSelector} implementation which performs random selection
 * over a {@link Linkable} (ideal peer sampler).
 * 
 * @author giuliano
 */
public class RandomSelectorOverLinkable implements IPushPeerSelector, Protocol {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	
	private static final String PAR_LINKABLE = "linkable";

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------
	
	private PermutingCache fCache;

	// ----------------------------------------------------------------------
	
	public RandomSelectorOverLinkable(IResolver resolver, String name) {
		this(resolver.getInt(name, PAR_LINKABLE));
	}
	
	// ----------------------------------------------------------------------
	
	public RandomSelectorOverLinkable(int pid) {
		fCache = new PermutingCache(pid);
	}
	
	// ----------------------------------------------------------------------
	
	public RandomSelectorOverLinkable(IReference<Linkable> neighborhood) {
		fCache = new PermutingCache(neighborhood);
	}
	
	// ----------------------------------------------------------------------
	// IPushPeerSelector interface.
	// ----------------------------------------------------------------------
	
	public Node selectPeer(Node source, ISelectionFilter filter,
			IGossipMessage scheduled) {
		return selectPeer(source, filter);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		fCache.populate(source, filter);
		fCache.shuffle();
		// Performs the selection.
		for (int i = 0; i < fCache.size(); i++) {
			Node candidate = fCache.get(i);
			if (filter.canSelect(source, candidate)) {
				return filter.selected(source, candidate);
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
