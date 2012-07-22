package it.unitn.disi.simulator.random;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class UniformDistribution implements IDistribution {

	private final Random fRandom;

	private final double fBase;

	private final double fRange;

	public UniformDistribution(@Attribute("Random") Random random) {
		this(random, 0, 1);
	}

	public UniformDistribution(@Attribute("Random") Random random,
			@Attribute("min") double min, @Attribute("max") double max) {

		if (min >= max) {
			throw new IllegalArgumentException(
					"Minimum must be smaller than maximum.");
		}
		fBase = min;
		fRange = max - min;
		fRandom = random;
	}

	@Override
	public double sample() {
		return fBase + fRandom.nextDouble() * fRange;
	}

	@Override
	public double expectation() {
		return fBase + (fRange / 2.0);
	}

}
