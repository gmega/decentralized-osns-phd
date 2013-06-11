package it.unitn.disi.simulator.measure;

/**
 * Simple interface for observing values.
 * 
 * @author giuliano
 */
public interface IValueObserver {

	public static IValueObserver NULL_OBSERVER = new IValueObserver() {
		@Override
		public void observe(double value) {
		}
	};

	public void observe(double value);

}
