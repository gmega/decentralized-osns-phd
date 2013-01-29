package it.unitn.disi.churn.connectivity.tce;

import java.io.Serializable;

class BFSQueue implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private int fRear = -1;

	private int fFront = -1;

	private int[] fQueue;

	public BFSQueue(int size) {
		ensure(size);
	}

	public void ensure(int size) {
		if (fQueue != null && size <= fQueue.length) {
			return;
		}

		fQueue = new int[size];
	}

	public int capacity() {
		return fQueue.length;
	}

	public boolean isEmpty() {
		return fFront == fRear;
	}

	public int peekFirst() {
		return fQueue[(fRear + 1) % fQueue.length];
	}

	public void addLast(int element) {
		fFront = (fFront + 1) % fQueue.length;
		fQueue[fFront] = element;
	}

	public int removeFirst() {
		fRear = (fRear + 1) % fQueue.length;
		return fQueue[fRear];
	}

}
