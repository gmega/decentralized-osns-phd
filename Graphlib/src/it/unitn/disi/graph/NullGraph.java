package it.unitn.disi.graph;

import java.util.Collection;
import java.util.Collections;

import peersim.graph.Graph;

public class NullGraph implements Graph {

	@Override
	public boolean isEdge(int i, int j) {
		return false;
	}

	@Override
	public Collection<Integer> getNeighbours(int i) {
		return Collections.emptyList();
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
	public int size() {
		return 0;
	}

	@Override
	public boolean directed() {
		return false;
	}

	@Override
	public boolean setEdge(int i, int j) {
		return false;
	}

	@Override
	public boolean clearEdge(int i, int j) {
		return false;
	}

	@Override
	public int degree(int i) {
		return 0;
	}

}
