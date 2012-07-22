package it.unitn.disi.simulator.random;

public abstract class UnitUniformDistribution implements IDistribution {

	@Override
	public abstract double sample();
	
	@Override
	public double expectation() {
		return 0.5;
	}

}
