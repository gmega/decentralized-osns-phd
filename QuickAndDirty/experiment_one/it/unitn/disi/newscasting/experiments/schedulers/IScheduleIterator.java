package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.Iterator;

public interface IScheduleIterator extends Iterator<Integer>{
	public int remaining();
}
