package it.unitn.disi.utils.collections;

import java.util.Iterator;

public abstract class AbstractReadaheadStream<K> implements Iterator<K>{

	private K fNext;
	
	public AbstractReadaheadStream() throws Exception {
		fNext = nextElement();
	}
	
	@Override
	public boolean hasNext() {
		return fNext == null;
	}

	@Override
	public K next() {
		K next = fNext;
		fNext = nextElement();
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	protected abstract K nextElement();
	
}
