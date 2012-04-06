package it.unitn.disi.newscasting.experiments.schedulers;

public class StaticScheduleIterator implements IScheduleIterator {
	
	private IStaticSchedule fSchedule;

	private int fIndex;
	
	public StaticScheduleIterator(IStaticSchedule schedule) {
		fSchedule = schedule;
	}

	@Override
	public Object nextIfAvailable() {
		if (fIndex == fSchedule.size()) {
			return DONE;
		}
		return fSchedule.get(fIndex++);
	}

	@Override
	public int remaining() {
		return fSchedule.size() - fIndex;
	}
	
}
