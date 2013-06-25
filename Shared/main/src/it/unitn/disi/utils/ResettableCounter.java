package it.unitn.disi.utils;

public class ResettableCounter {
	
	private final int fInitial;
	
	private int fCounter;
	
	public ResettableCounter(int initial) {
		this(initial, initial);
	}
	
	public ResettableCounter(int resetValue, int initial) {
		fInitial = resetValue;
		fCounter = initial;
	}
	
	public void decrement() {
		fCounter--;
	}

	public int get() {
		return fCounter;
	}
	
	public void reset() {
		fCounter = fInitial;
	}
}
