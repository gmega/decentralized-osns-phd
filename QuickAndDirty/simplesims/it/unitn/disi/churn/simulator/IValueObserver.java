package it.unitn.disi.churn.simulator;

import java.io.PrintStream;

public interface IValueObserver {
	
	public static IValueObserver NULL_OBSERVER = new IValueObserver() {
		@Override
		public void observe(double value) {
		}
		
		@Override
		public void print(PrintStream out) {
			out.println("NULL");
		}
	};

	public void observe(double value);
	
	public void print(PrintStream out);
	
}
