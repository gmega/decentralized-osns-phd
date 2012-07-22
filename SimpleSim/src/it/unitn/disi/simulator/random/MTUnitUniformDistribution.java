package it.unitn.disi.simulator.random;

public class MTUnitUniformDistribution extends UnitUniformDistribution{
	
	private final MersenneTwister fRandom;
	
	public MTUnitUniformDistribution(MersenneTwister random) {
		fRandom = random;
	}

	@Override
	public double sample() {
		return fRandom.nextDouble();
	}
}
