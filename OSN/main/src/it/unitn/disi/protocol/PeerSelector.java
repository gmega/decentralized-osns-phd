package it.unitn.disi.protocol;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import it.unitn.disi.protocol.selectors.ISelectionFilter;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * PeerSelector is a utility class for helping bridge a {@link Linkable} and an
 * {@link IPeerSamplingService}. It merely keeps track of which elements in the
 * cache have already been selected.
 * 
 * XXX There is ample room for optimizations, but I wanted to keep this simple
 * and <b>bug free</b> above all.
 * 
 * @author giuliano
 */
public class PeerSelector implements ISelectionFilter {

	private Set<Node> fVetoed = new HashSet<Node>();

	private Set<Node> fInView = new HashSet<Node>();

	public void update(Linkable linkable) {
		int degree = linkable.degree();
		for (int i = 0; i < degree; i++) {
			fInView.add(linkable.getNeighbor(i));
		}

		Iterator<Node> vetoedIt = fVetoed.iterator();
		while (vetoedIt.hasNext()) {
			Node vetoed = vetoedIt.next();
			if (!fInView.contains(vetoed)) {
				vetoedIt.remove();
			}
		}

		fInView.clear();
	}

	public boolean canSelect(Node node) {
		return !fVetoed.contains(node);
	}

	public Node selected(Node node) {
		fVetoed.add(node);
		return node;
	}
	
	public Object clone() {
		try {
			PeerSelector cloned = (PeerSelector) super.clone();
			cloned.fVetoed = new HashSet<Node>(this.fVetoed);
			cloned.fInView = new HashSet<Node>(this.fInView);
			
			return cloned;
		} catch(CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
