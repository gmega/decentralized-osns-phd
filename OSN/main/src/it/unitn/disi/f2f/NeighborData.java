package it.unitn.disi.f2f;

import peersim.core.CommonState;
import peersim.core.Node;

class NeighborData {

	private Node fNode;

	private int fIndex;

	private long fLastSeen = Integer.MIN_VALUE;

	private int fEvictionThreshold;

	private int fBackoff = 0;

	public NeighborData(Node node, int index, int evictionThreshold) {
		fIndex = index;
		setThreshold(evictionThreshold);
	}

	public void nextCycle() {
		fBackoff = Math.max(0, fBackoff - 1);
	}

	public void found() {
		fLastSeen = CommonState.getTime();
		fBackoff = 0;
	}

	public void notFound() {
		fBackoff = Math.max(1, 2 * fBackoff);
	}

	public boolean isElligible() {
		return !isInView() && fBackoff == 0;
	}

	public boolean isInView() {
		return (CommonState.getTime() - fLastSeen) <= fEvictionThreshold;
	}

	public void setThreshold(int evictionThreshold) {
		fEvictionThreshold = evictionThreshold;
	}

	public int index() {
		return fIndex;
	}
	
	public Node node() {
		return fNode;
	}
}
