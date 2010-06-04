package it.unitn.disi.application.probabrm;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.utils.PermutingCache;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Simple protocol which works by presenting a shuffled view over a
 * {@link Linkable}.
 * 
 * @author giuliano
 */
public class ShufflingLinkable implements IDynamicLinkable, CDProtocol {

	private static final String PAR_LINKABLE = "linkable";
	
	private PermutingCache fCache;
	
	private int fLastShuffle;
	
	public ShufflingLinkable(String prefix) {
		fCache = new PermutingCache(Configuration.getPid(prefix + "." + PAR_LINKABLE));
	}

	public int degree() {
		return fCache.size();
	}

	public Node getNeighbor(int i) {
		return fCache.get(i);
	}

	public void nextCycle(Node node, int protocolID) {
		fCache.shuffle(node);
		fLastShuffle = CommonState.getIntTime();
	}
	
	public boolean hasChanged(int time) {
		return time <= fLastShuffle;
	}
	
	public boolean contains(Node neighbor) {
		return fCache.contains(neighbor);
	}

	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException("This linkable is read-only.");
	}
	
	public void onKill() { }
	
	public void pack() { }
	
	public Object clone() {
		try {
			ShufflingLinkable cloned = (ShufflingLinkable) super.clone();
			cloned.fCache = (PermutingCache) this.fCache.clone();
			return cloned;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
