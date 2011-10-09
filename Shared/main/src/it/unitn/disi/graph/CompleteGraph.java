package it.unitn.disi.graph;

import java.util.ArrayList;
import java.util.Collection;

public class CompleteGraph implements IndexedNeighborGraph {

	private final int fSize;

	public CompleteGraph(int size) {
		fSize = size;
	}

	@Override
	public boolean isEdge(int i, int j) {
		check(i);
		check(j);
		return i != j;
	}

	@Override
	public Collection<Integer> getNeighbours(int i) {
		check(i);
		ArrayList<Integer> neighbors = new ArrayList<Integer>();
		for (int j = 0; j < fSize; j++) {
			if (j != i) {
				neighbors.add(j);
			}
		}
		return neighbors;
	}

	@Override
	public int size() {
		return fSize;
	}

	@Override
	public boolean directed() {
		return false;
	}

	@Override
	public int degree(int i) {
		check(i);
		return fSize - 1;
	}

	@Override
	public boolean setEdge(int i, int j) {
		throw new UnsupportedOperationException("This graph is read-only.");
	}

	@Override
	public boolean clearEdge(int i, int j) {
		throw new UnsupportedOperationException("This graph is read-only.");
	}

	@Override
	public Object getNode(int i) {
		return null;
	}

	@Override
	public Object getEdge(int i, int j) {
		return null;
	}

	@Override
	public int getNeighbor(int nodeIndex, int neighborIndex) {
		check(nodeIndex);
		check(neighborIndex, degree(nodeIndex));
		return neighborIndex < nodeIndex ? neighborIndex : (neighborIndex + 1);
	}

	private void check(int nodeIndex) {
		check(nodeIndex, fSize);
	}

	private void check(int index, int bound) {
		if (index < 0 || index >= bound) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
	}
}
