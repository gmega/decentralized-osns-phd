package it.unitn.disi.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.common.collect.PeekingIterator;

/**
 * Wrapper over an {@link Iterator} which provides a peek operation.
 * 
 * @author giuliano
 */
public class PeekingIteratorAdapter<E> implements PeekingIterator<E>{
	
	private final Iterator<E> fDelegate;
	
	private E fLookahead;
	
	public PeekingIteratorAdapter(Iterator<E> delegate) {
		fDelegate = delegate;
		this.updateLookahead();
	}

	@Override
	public boolean hasNext() {
		return fLookahead != null;
	}

	@Override
	public E next() {
		elementCheck();
		E next = fLookahead;
		this.updateLookahead();
		return next;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public E peek() {
		elementCheck();
		return fLookahead;
	}

	private void elementCheck() {
		if (fLookahead == null) {
			throw new NoSuchElementException();
		}
	}
	
	private void updateLookahead() {
		if (fDelegate.hasNext()) {
			fLookahead = fDelegate.next();
		} else {
			fLookahead = null;
		}
	}
}
