package it.unitn.disi.newscasting.experiments.schedulers;

import java.util.Iterator;

public class RepetitionDecorator implements Iterable<Integer> {

	private Iterable<Integer> fDelegate;

	private final int fRepetitions;

	public RepetitionDecorator(Iterable<Integer> delegate, int repetitions) {
		fDelegate = delegate;
		fRepetitions = repetitions;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new RepetitionIterator(fDelegate, fRepetitions);
	}

	static class RepetitionIterator implements IScheduleIterator {

		private Iterable<Integer> fGenerator;

		private IScheduleIterator fIterator;

		private int fRemaining;

		private int fBaseRemaining;

		public RepetitionIterator(Iterable<Integer> fDelegate, int repetitions) {
			fGenerator = fDelegate;
			fIterator = (IScheduleIterator) fDelegate.iterator();
			fBaseRemaining = fIterator.remaining();
			fRemaining = repetitions;
		}

		@Override
		public boolean hasNext() {
			while (!fIterator.hasNext() && fRemaining > 0) {
				fIterator = (IScheduleIterator) fGenerator.iterator();
				fRemaining--;
			}
			return fIterator.hasNext();
		}

		@Override
		public Integer next() {
			return fIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int remaining() {
			// XXX This might not be always correct if the delegate schedule
			// isn't static.
			return (fBaseRemaining * (fRemaining - 1)) + fIterator.remaining();
		}

	}
}
