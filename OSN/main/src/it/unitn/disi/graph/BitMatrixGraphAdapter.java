package it.unitn.disi.graph;

import java.util.BitSet;

import it.unitn.disi.utils.graph.IndexedNeighborGraph;
import peersim.graph.BitMatrixGraph;

/**
 * Provides an {@link IndexedNeighborGraph} implementation on top of
 * {@link BitMatrixGraph} which can still guarantee some efficiency as long as
 * the {@link #getNeighbor(int, int)} operation is used inside loops, to access
 * neighbors sequentially.
 * 
 * @author giuliano
 */
public class BitMatrixGraphAdapter extends BitMatrixGraph implements IndexedNeighborGraph {
	
	private final int [] fCache;
	
	private int fDegree = -1; 
	
	private Integer fId = null;

	public BitMatrixGraphAdapter(int n) {
		this(n, true);
	}
	
	public BitMatrixGraphAdapter(int n, boolean directed) {
		super(n, directed);
		fCache = new int[n];
	}

	@Override
	public int getNeighbor(int nodeIndex, int neighborIndex) {
		if (nodeIndex != fId) {
			fDegree = 0;
			BitSet bs = set(nodeIndex);
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
				fCache[fDegree++] = i;
			}
		}
		
		if (neighborIndex >= fDegree) {
			throw new ArrayIndexOutOfBoundsException(neighborIndex);
		}
		
		return fCache[neighborIndex];
	}
	
}
