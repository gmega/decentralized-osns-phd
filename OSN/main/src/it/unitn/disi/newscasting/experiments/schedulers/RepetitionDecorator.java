package it.unitn.disi.newscasting.experiments.schedulers;

public class RepetitionDecorator implements ISchedule {

	private ISchedule fDelegate;

	private final int fRepetitions;

	private final boolean fLoop;

	public RepetitionDecorator(ISchedule delegate, int repetitions, boolean loop) {
		fDelegate = delegate;
		fRepetitions = repetitions;
		fLoop = loop;
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
		return fLoop ? new StreamLoopRepetition(fDelegate, fRepetitions)
				: new SequentialRepetition(fDelegate, fRepetitions);
	}

	static class SequentialRepetition implements IScheduleIterator {

		private IScheduleIterator fDelegate;

		private int fRepetitions;

		private int fCurrentRepeat = 0;

		private Object fCurrent;

		public SequentialRepetition(ISchedule delegate, int repetitions) {
			fDelegate = delegate.iterator();
			fRepetitions = repetitions;
		}

		@Override
		public Object nextIfAvailable() {
			if (fCurrentRepeat == 0) {
				Object next = fDelegate.nextIfAvailable();
				if (next == IScheduleIterator.DONE) {
					return IScheduleIterator.DONE;
				}
				fCurrentRepeat = fRepetitions;
				fCurrent = next;
			}

			fCurrentRepeat--;
			return fCurrent;
		}

		@Override
		public int remaining() {
			// XXX This might not be always correct if the delegate schedule
			// isn't static.
			return fDelegate.remaining() * fRepetitions + fCurrentRepeat;
		}

	}

	static class StreamLoopRepetition implements IScheduleIterator {

		private ISchedule fGenerator;

		private IScheduleIterator fIterator;

		private int fRemaining;

		private int fBaseRemaining;

		public StreamLoopRepetition(ISchedule delegate, int repetitions) {
			fGenerator = delegate;
			fIterator = delegate.iterator();
			fBaseRemaining = fIterator.remaining();
			fRemaining = repetitions;
		}

		@Override
		public Object nextIfAvailable() {
			Object next;
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
