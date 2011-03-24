package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import peersim.core.Network;
import peersim.core.Node;

public class OrderedFullNetworkScheduler implements Iterable<Integer> {

	private Node[] fPermutation;

	public OrderedFullNetworkScheduler(Comparator<Node> comparator) {
		fPermutation = new Node[networkSize()];
		for (int i = 0; i < fPermutation.length; i++) {
			fPermutation[i] = get(i);
		}
		Arrays.sort(fPermutation, comparator);
	}

	@Override
	public Iterator<Integer> iterator() {
		Schedule sched = new Schedule();
		sched.initialize();
		return sched;
	}

	protected int networkSize() {
		return Network.size();
	}

	protected Node get(int index) {
		return Network.get(index);
	}

	private Node getOrdered(int index) {
		return fPermutation[index];
	}

	class Schedule implements Iterator<Integer> {

		private boolean[] fSelected;

		private int fRemaining;

		public void initialize() {
			int size = networkSize();
			fSelected = new boolean[size];
			fRemaining = size;
		}

		@Override
		public boolean hasNext() {
			return fRemaining != 0;
		}

		@Override
		public Integer next() {
			for (int i = 0; i < fSelected.length; i++) {
				if (!fSelected[i]) {
					Node candidate = getOrdered(i);
					if (candidate.isUp()) {
						fRemaining--;
						return (int) candidate.getID();
					}
				}
			}
			return null;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}
