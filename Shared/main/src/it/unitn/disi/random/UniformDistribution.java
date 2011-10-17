package it.unitn.disi.random;

import java.util.Random;

public class UniformDistribution implements IDistribution {
	
	private final Random fRandom;

	public UniformDistribution(Random random) {
		fRandom = random;
	}
	
	@Override
	public double sample() {
		return fRandom.nextDouble();
	}

	@Override
	public double expectation() {
		return 0.5;
	}

}
