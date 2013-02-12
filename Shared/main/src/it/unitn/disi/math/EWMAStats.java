package it.unitn.disi.math;

/**
 * Implements an <a href=
 * "http://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average"
 * >exponential moving average</a>.
 * 
 * @author giuliano
 */
public class EWMAStats {

	private static final int SWITCHING_POINT = 4;

	private final double fAlpha;

	private long fN;

	private double fAverage;

	/**
	 * @param n
	 *            the latest 'n' elements are the ones influencing the average
	 *            the most (about 86% of its value).
	 */
	public EWMAStats(int n) {
		this(2.0 / (n + 1));
	}

	public EWMAStats(double alpha) {
		fAlpha = alpha;
	}

	public void add(double value) {
		fN++;
		if (fN > SWITCHING_POINT) {
			fAverage = fAlpha * value + (1.0 - fAlpha) * fAverage;
		} else {
			fAverage += value;
			if (fN == SWITCHING_POINT) {
				fAverage = fAverage / fN;
			}
		}
	}

	public double getAverage() {
		if (fN >= SWITCHING_POINT) {
			return fAverage;
		} else {
			return fAverage / fN;
		}
	}
}
