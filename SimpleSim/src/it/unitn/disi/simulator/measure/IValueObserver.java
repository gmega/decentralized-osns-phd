package it.unitn.disi.simulator.measure;

public interface IValueObserver {
	
	public static IValueObserver NULL_OBSERVER = new IValueObserver() {
		@Override
		public void observe(double value) {
		}
	};

	public void observe(double value);
	
}
