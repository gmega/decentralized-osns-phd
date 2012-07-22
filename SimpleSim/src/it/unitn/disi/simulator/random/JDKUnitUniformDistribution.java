package it.unitn.disi.simulator.random;

import java.util.Random;

public class JDKUnitUniformDistribution extends UnitUniformDistribution {

	private final Random fRandom;
	
	public JDKUnitUniformDistribution(Random random) {
		fRandom = random;
	}
	
	@Override
	public double sample() {
		return fRandom.nextDouble();
	}

}
