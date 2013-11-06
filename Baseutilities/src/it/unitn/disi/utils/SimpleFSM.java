package it.unitn.disi.utils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class SimpleFSM <T extends Enum<?>>{
	
	private Multimap<T, T> fTransitions;
	
	private T fFrom;
	
	private boolean fDeterministic = true;
	
	public SimpleFSM(Class<T> klass) {
		fTransitions = HashMultimap.create();
	}
	
	public SimpleFSM<T> allowTransitionFrom(T from) {
		fFrom = from;
		return this;
	}
	
	public void to(T...allowedStates) {
		if (fFrom == null) {
			throw new IllegalStateException("Need to call #allowTransitionFrom first.");
		}
		for (T state : allowedStates) {
			fTransitions.put(fFrom, state);
		}
		
		if (allowedStates.length > 1) {
			fDeterministic = false;
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
	
	public T transition(T current) {
		if (!fDeterministic) {
			return null;
		}
		return fTransitions.get(current).iterator().next();
	}
}
