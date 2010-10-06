package it.unitn.disi.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class SimpleFSM <T extends Enum<?>>{
	
	private Multimap<T, T> fTransitions;
	
	private T fFrom;
	
	public SimpleFSM(Class<T> klass) {
		fTransitions = HashMultimap.create();
	}
	
	public SimpleFSM<T> allowTransitionFrom(T from) {
		fFrom = from;
		return this;
	}
	
	public void to(T...allowedStates) {
		if (fFrom == null) {
			throw new IllegalStateException("Need to call #from first.");
		}
		for (T state : allowedStates) {
			fTransitions.put(fFrom, state);
		}
		fFrom = null;
	}
	
	public T checkedTransition(T current, T next) {
		if (!fTransitions.containsEntry(current, next)) {
			throw new IllegalStateException("Illegal transition " + this
					+ " => " + "next.");
		}
		return next;
	}
}
