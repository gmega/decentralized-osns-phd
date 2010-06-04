package it.unitn.disi;

import java.util.Random;

public class RouletteWheel implements Cloneable {

	private double[] fProbabilities;

	private Random fRandom;

	public RouletteWheel(double[] probabilities, Random r) {
		if (probabilities.length == 0) {
			throw new IllegalArgumentException("Probability vector must be of size >= 1.");
		}
		
		fProbabilities = new double[probabilities.length];
		// Computes the sum of the probabilities to use as normalization
		// factor.
		System.arraycopy(probabilities, 0, fProbabilities, 0,
				probabilities.length);
		for (int i = 1; i < fProbabilities.length; i++) {
			fProbabilities[i] += fProbabilities[i - 1];
		}

		fRandom = r;
	}

	public int spin() {
		double draw = fRandom.nextDouble()
				* fProbabilities[fProbabilities.length - 1];

		double lower = 0.0;
		for (int i = 0; i < fProbabilities.length; i++) {
			if (draw >= lower && draw < fProbabilities[i]) {
				return i;
			}
			lower = fProbabilities[i];
		}

		// Should never reach here.
		throw new IllegalStateException("Internal error.");
	}

	public int bins() {
		return fProbabilities.length;
	}

	public Object clone() {
		// Since we're immutable, return ourselves.
		return this;
	}
}
