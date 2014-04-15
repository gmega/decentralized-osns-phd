package it.unitn.disi.graph.peersim;

import it.unitn.disi.graph.IGraphVisitor;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.algorithms.VisitorSupport;
import it.unitn.disi.utils.collections.FastGetBitset;
import peersim.graph.BitMatrixGraph;

/**
 * Provides an {@link IndexedNeighborGraph} implementation on top of
 * {@link BitMatrixGraph} which can still guarantee some efficiency as long as
 * the {@link #getNeighbor(int, int)} operation is used inside loops, to access
 * neighbors sequentially.
 * 
 * @author giuliano
 */
public class BitMatrixGraphAdapter extends FastBitMatrixGraph implements IndexedNeighborGraph {
	
	private final int [] fCache;
	
	private int fDegree = -1; 
	
	private int fId = -1;

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
			FastGetBitset bs = sets[nodeIndex];
			for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
				fCache[fDegree++] = i;
			}
			fId = nodeIndex;
		}
		
		if (neighborIndex >= fDegree) {
			throw new ArrayIndexOutOfBoundsException(neighborIndex);
		}
		
		return fCache[neighborIndex];
	}

	@Override
	public void visit(IGraphVisitor visitor) {
		VisitorSupport support = new VisitorSupport(this, directed);
		support.visit(visitor);
	}
	
}
