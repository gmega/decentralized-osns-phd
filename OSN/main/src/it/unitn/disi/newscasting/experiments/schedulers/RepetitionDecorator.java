package it.unitn.disi.newscasting.experiments.schedulers;

public class RepetitionDecorator implements ISchedule {

	private ISchedule fDelegate;

	private final int fRepetitions;

	public RepetitionDecorator(ISchedule delegate, int repetitions) {
		fDelegate = delegate;
		fRepetitions = repetitions;
	}
	
	@Override
	public int size() {
		if (fDelegate.size() == ISchedule.UNKNOWN) {
			return ISchedule.UNKNOWN;
		} else {
			return fDelegate.size() * fRepetitions;
		}
	}

	@Override
	public IScheduleIterator iterator() {
		return new RepetitionIterator(fDelegate, fRepetitions);
	}

	static class RepetitionIterator implements IScheduleIterator {

		private ISchedule fGenerator;

		private IScheduleIterator fIterator;

		private int fRemaining;

		private int fBaseRemaining;

		public RepetitionIterator(ISchedule delegate, int repetitions) {
			fGenerator = delegate;
			fIterator = delegate.iterator();
			fBaseRemaining = fIterator.remaining();
			fRemaining = repetitions;
		}

		@Override
		public Integer nextIfAvailable() {
			Integer next;
			while (((next = fIterator.nextIfAvailable()) == IScheduleIterator.DONE)
					&& fRemaining > 0) {
				fIterator = (IScheduleIterator) fGenerator.iterator();
				fRemaining--;
			}
			return next;
		}

		@Override
		public int remaining() {
			// XXX This might not be always correct if the delegate schedule
			// isn't static.
			return (fBaseRemaining * (fRemaining - 1)) + fIterator.remaining();
		}

	}
}
