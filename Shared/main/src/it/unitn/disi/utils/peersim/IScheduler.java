package it.unitn.disi.utils.peersim;

import peersim.core.Node;

public interface IScheduler<T> {
	public void schedule(long time, int pid, Node source, T event);
}
