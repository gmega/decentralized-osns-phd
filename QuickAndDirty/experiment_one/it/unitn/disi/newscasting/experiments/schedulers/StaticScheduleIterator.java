package it.unitn.disi.newscasting.experiments.schedulers;

public class StaticScheduleIterator implements IScheduleIterator {
	
	private IStaticSchedule fSchedule;

	private int fIndex;
	
	public StaticScheduleIterator(IStaticSchedule schedule) {
		fSchedule = schedule;
	}

	@Override
	public boolean hasNext() {
		return fIndex != fSchedule.size();
	}

	@Override
	public Integer next() {
		return fSchedule.get(fIndex++);
	}

	@Override
	public int remaining() {
		return fSchedule.size() - fIndex;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
