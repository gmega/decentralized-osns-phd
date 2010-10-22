package it.unitn.disi.sps;

import java.util.Arrays;

import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.peersim.PermutingCache;
import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Reference peer sampling which produces perfectly random samples of the
 * network.
 * 
 * @author giuliano
 */
@AutoConfig
public class PerfectF2FPeerSampling implements Linkable, CDProtocol {

	private int fTwoHop;
	
	private int fViewSize;
	
	private int fReplacement;
	
	private PermutingCache fCache;

	private Node [] fView;
	
	public PerfectF2FPeerSampling(
			@Attribute("twohop") int twoHop,
			@Attribute("view_size") int viewSize,
			@Attribute("l") int replacement) {
		fTwoHop = twoHop;
		fCache = new PermutingCache(twoHop);
		fViewSize = viewSize;
		fReplacement = replacement;
	}
	
	public void init(Node node) {
		Linkable sn = (Linkable) node.getProtocol(fTwoHop);
		fView = new Node[Math.min(sn.degree(), fViewSize)];
		sample(node, fView.length);
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		sample(node, fReplacement);
	}
	
	private void sample(Node node, int howMany) {
		fCache.populate(node);
		fCache.shuffle();
		for (int i = 0; i < howMany; i++) {
			fView[i] = fCache.get(i);
		}
		OrderingUtils.permute(0, fView.length, fView, CommonState.r);
		fCache.invalidate();
	}

	@Override
	public int degree() {
		return fView.length;
	}

	@Override
	public Node getNeighbor(int i) {
		return fView[i];
	}
	
	@Override
	public boolean contains(Node neighbor) {
		for (Node node : fView) {
			if (node.equals(neighbor)) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onKill() {
		fView = null;
	}

	@Override
	public void pack() { }
	
	public Object clone() {
		try {
			PerfectF2FPeerSampling cloned = (PerfectF2FPeerSampling) super.clone();
			if (fView != null) {
				cloned.fView = Arrays.copyOf(fView, fView.length);
			}
			return cloned;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}

}
