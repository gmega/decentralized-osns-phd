package it.unitn.disi.utils;

import java.util.Comparator;

public interface IMultiCounter<K> extends Comparator<K> {

	public abstract void increment(K id);

	public abstract void decrement(K id);

	public abstract void increment(K id, int increment);

	public abstract void decrement(K id, int decrement);

	public abstract int count(K id);

}