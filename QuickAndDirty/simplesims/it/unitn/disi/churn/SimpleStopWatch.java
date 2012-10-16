package it.unitn.disi.churn;

class SimpleStopWatch {
	
	private double fLap;
	
	public void start(double time) {
		fLap = time;
	}
	
	public double stop(double time) {
		if(!isCounting()) {
			throw new IllegalStateException();
		}
		
		double value = check(fLap - time);
		fLap = Double.MIN_VALUE;
		return value;
	}

	public boolean isCounting() {
		return fLap != Double.MIN_VALUE;
	}

	private double check(double d) {
		if (d < 0) {
			throw new IllegalArgumentException("Incorrect time setting.");
		}
		return d;
	}

}
