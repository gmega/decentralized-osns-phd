package it.unitn.disi.util;

import java.util.Random;

/**
 * A {@link RouletteWheel} can be spun so as to generate events with different
 * predefined probabilities. <BR> {@link RouletteWheel} is immutable and
 * thread-safe.
 * 
 * @author giuliano
 */
public class RouletteWheel implements Cloneable {

	// ----------------------------------------------------------------------
	// Members.
	// ----------------------------------------------------------------------

	/**
	 * Vector defining the probability of each event.
	 */
	private final double[] fProbabilities;

	/**
	 * Random number generator.
	 */
	private final Random fRandom;

	// ----------------------------------------------------------------------

	/**
	 * Constructs a new {@link RouletteWheel}.
	 * 
	 * @param probabilities
	 *            a vector defining the probability for each of the bins in the
	 *            roulette wheel.
	 * @param r
	 *            a random number generator.
	 */
	public RouletteWheel(double[] probabilities, Random r) {
		if (probabilities.length == 0) {
			throw new IllegalArgumentException(
					"Probability vector must be of size >= 1.");
		}

		fProbabilities = new double[probabilities.length];
		System.arraycopy(probabilities, 0, fProbabilities, 0,
				probabilities.length);
		for (int i = 1; i < fProbabilities.length; i++) {
			fProbabilities[i] += fProbabilities[i - 1];
		}

		fRandom = r;
	}

	// ----------------------------------------------------------------------

	/**
	 * Spins the wheel.
	 * 
	 * @return the selected bin. Bins are selected according to the
	 *         probabilities defined in the constructor.
	 */
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

	// ----------------------------------------------------------------------

	/**
	 * @return the number of bins in this roulette wheel.
	 */
	public int bins() {
		return fProbabilities.length;
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------
	
	public Object clone() {
		// Since we're immutable, return ourselves.
		return this;
	}

	// ----------------------------------------------------------------------

}
