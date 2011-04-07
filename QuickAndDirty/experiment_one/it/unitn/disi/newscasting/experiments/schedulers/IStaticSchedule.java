package it.unitn.disi.newscasting.experiments.schedulers;

/**
 * An {@link IStaticSchedule} is a special kind of {@link ISchedule} with a
 * pre-defined ordering, which never changes in time.
 * 
 * @author giuliano
 */
public interface IStaticSchedule extends ISchedule {
	public int get(int index);
}
