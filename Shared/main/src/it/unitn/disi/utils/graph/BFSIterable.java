package it.unitn.disi.utils.graph;

import it.unitn.disi.utils.collections.Pair;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class BFSIterable implements Iterable<Pair<Integer, Integer>> {

	private final IndexedNeighborGraph fGraph;
	
	private final int fRoot;

	public BFSIterable(IndexedNeighborGraph graph, int root) {
		fGraph = graph;
		fRoot = root;
	}
	
	public Iterator<Pair<Integer, Integer>> iterator() {
		return new BFSIterator(fGraph, fRoot);
	}

	public static class BFSIterator implements Iterator<Pair<Integer, Integer>> {

		private final IndexedNeighborGraph fGraph;

		private final LinkedList<Pair<Integer, Integer>> fQueue;

		
		private final BitSet fQueued;

		public BFSIterator(IndexedNeighborGraph graph, int root) {
			fGraph = graph;
			fQueue = new LinkedList<Pair<Integer, Integer>>();
			fQueued = new BitSet(graph.size());
			this.process(new Pair<Integer, Integer>(root, 0));
		}

		@Override
		public boolean hasNext() {
			return !fQueue.isEmpty();
		}

		@Override
		public Pair<Integer, Integer> next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			Pair<Integer, Integer> next = fQueue.removeFirst();
			this.process(next);
			return next;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void process(Pair<Integer, Integer> pair) {
			int id = pair.a;
			int depth = pair.b;
			
			int degree = fGraph.degree(id);
			for (int i = 0; i < degree; i++) {
				int neighbor = fGraph.getNeighbor(id, i);
				if (!fQueued.get(neighbor)) {
					fQueue.add(new Pair<Integer, Integer>(neighbor, depth + 1));
					fQueued.set(neighbor);
				}
			}
		}
	}
}
