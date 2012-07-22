package it.unitn.disi.simulator.churnmodel.avt;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArrayIterator implements Iterator<Long> {
	
	private final long[] fArray;

	private int fIdx;

	private long fSync;

	public ArrayIterator(long[] array) {
		fArray = array;
		fSync = -1;
	}

	public void setSync(long sync) {
		fSync = sync;
	}

	@Override
	public boolean hasNext() {
		return hasMoreEvents() || isInfinite();
	}

	@Override
	public Long next() {
		if (!hasMoreEvents()) {
			resync();
		}
		return fArray[fIdx++];
	}

	private boolean hasMoreEvents() {
		return fIdx != fArray.length;
	}

	private boolean isInfinite() {
		return fSync > 0;
	}

	private void resync() {
		if (fSync < 0) {
			throw new NoSuchElementException();
		}
		// Shifts events to the synchronization point.
		for (int i = 0; i < fArray.length; i++) {
			fArray[i] += fSync;
		}
		fIdx = 0;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
