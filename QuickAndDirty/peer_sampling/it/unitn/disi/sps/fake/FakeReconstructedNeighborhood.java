package it.unitn.disi.sps.fake;

import java.util.Arrays;

import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.IInitializable;
import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Extremely simple model for reconstruction overhead incurred by our peer sampling protocol.
 * 
 * @author giuliano
 */
@AutoConfig
public class FakeReconstructedNeighborhood implements IInitializable, Linkable,
		CDProtocol {

	@Attribute("linkable")
	private int fLinkable;

	/**
	 * Last time step when the node entered the network.
	 */
	private long fTimeBase;

	/**
	 * Fixed reconstruction overhead times.
	 */
	private int[] fReconstructionTimes;

	/** 
	 * Current neighbors of the node.
	 */
	private Node[] fCurrentNeighbors;

	// ----------------------------------------------------------------------
	
	public FakeReconstructedNeighborhood() { }
	
	// ----------------------------------------------------------------------
	
	public void initialize(Node node) {
		int degree = ((Linkable) node.getProtocol(fLinkable)).degree();
		fReconstructionTimes = new int[degree];
		fCurrentNeighbors = new Node[degree];
		Arrays.fill(fReconstructionTimes, -1);
		reinitialize();
	}
	
	// ----------------------------------------------------------------------
	
	public void setReconstructionTime(Node neighbor, int value) {
		int idx = indexOf(neighbor);
		if (fReconstructionTimes[idx] != -1) {
			throw new IllegalStateException("Duplicate value.");
		}
		fReconstructionTimes[idx] = value;
	}
	
	// ----------------------------------------------------------------------

	public void reinitialize() {
		Arrays.fill(fCurrentNeighbors, null);
		fTimeBase = CommonState.getTime();
	}
	
	// ----------------------------------------------------------------------
	// Linkable interface.
	// ----------------------------------------------------------------------

	@Override
	public void onKill() {
		fReconstructionTimes = null;
		fCurrentNeighbors = null;
	}
	
	// ----------------------------------------------------------------------

	@Override
	public void nextCycle(Node node, int protocolID) {
		long time = CommonState.getTime();
		for (int i = 0; i < fReconstructionTimes.length; i++) {
			if (fTimeBase + fReconstructionTimes[i] == time) {
				Linkable linkable = (Linkable) node.getProtocol(fLinkable);
				fCurrentNeighbors[degree()] = linkable.getNeighbor(i);
			}
		}
	}
	
	// ----------------------------------------------------------------------

	@Override
	public int degree() {
		int degree = fCurrentNeighbors.length - 1;
		for (; degree >= 0; degree--) {
			if (fCurrentNeighbors[degree] != null) {
				break;
			}
		}

		return degree + 1;
	}
	
	// ----------------------------------------------------------------------

	@Override
	public Node getNeighbor(int i) {
		return fCurrentNeighbors[i];
	}
	
	// ----------------------------------------------------------------------

	@Override
	public boolean contains(Node neighbor) {
		return indexOf(neighbor) != -1;
	}
	
	// ----------------------------------------------------------------------

	@Override
	public void pack() {
		// Nothing.
	}
	
	// ----------------------------------------------------------------------

	@Override
	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException();
	}
	
	// ----------------------------------------------------------------------
	
	private int indexOf(Node neighbor) {
		int degree = degree();
		for (int i = 0; i < degree; i++) {
			if (neighbor.equals(fCurrentNeighbors[i])) {
				return i;
			}
		}

		return -1;
	}
	
	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			FakeReconstructedNeighborhood clone = (FakeReconstructedNeighborhood) super.clone();
			clone.fReconstructionTimes = Arrays.copyOf(fReconstructionTimes, fReconstructionTimes.length);
			clone.fCurrentNeighbors = Arrays.copyOf(fCurrentNeighbors, fCurrentNeighbors.length);
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
}
