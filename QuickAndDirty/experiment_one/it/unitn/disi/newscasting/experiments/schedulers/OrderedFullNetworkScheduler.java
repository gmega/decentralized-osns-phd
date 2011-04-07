package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import peersim.core.Network;
import peersim.core.Node;

public class OrderedFullNetworkScheduler implements IStaticSchedule {

	private Node[] fPermutation;

	public OrderedFullNetworkScheduler() {
		this(new NodeIdOrdering());
	}

	public OrderedFullNetworkScheduler(Comparator<Node> comparator) {
		fPermutation = new Node[networkSize()];
		for (int i = 0; i < fPermutation.length; i++) {
			fPermutation[i] = get0(i);
		}
		Arrays.sort(fPermutation, comparator);
	}
	
	public int size() {
		return fPermutation.length;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new StaticScheduleIterator(this);
	}

	protected int networkSize() {
		return Network.size();
	}

	public int get(int index) {
		return (int) fPermutation[index].getID();
	}

	protected Node get0(int i) {
		return Network.get(i);
	}

	static class NodeIdOrdering implements Comparator<Node> {
		@Override
		public int compare(Node o1, Node o2) {
			return (int) Math.signum(o1.getID() - o2.getID());
		}
	}
}
