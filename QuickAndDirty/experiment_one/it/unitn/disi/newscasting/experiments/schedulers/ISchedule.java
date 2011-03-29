package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.Iterator;

public interface ISchedule extends Iterator<Integer>{
	public int remaining();
}
