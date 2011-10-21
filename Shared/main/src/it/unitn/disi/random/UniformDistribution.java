package it.unitn.disi.random;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class UniformDistribution implements IDistribution {

	private final Random fRandom;

	public UniformDistribution(@Attribute("Random") Random random) {
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
