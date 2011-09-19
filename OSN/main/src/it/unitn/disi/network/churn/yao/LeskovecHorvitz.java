package it.unitn.disi.network.churn.yao;

import java.util.Random;

import peersim.config.Attribute;

import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.random.IDistribution;

/**
 * Random number generators for distributions taken from Leskovec and Horvitz's
 * <a href="http://dx.doi.org/10.1145/1367497.1367620">Planetary-scale views on
 * large instant-messaging network</a>.
 * 
 * @author giuliano
 */
public class LeskovecHorvitz implements IDistributionGenerator {

	private final double UP_K = 547.4608;
	private final double UP_ALPHA = -(1.0 / 0.77);

	private final double DOWN_K = 16.18612;
	private final double DOWN_ALPHA = -(1.0 / 0.34);

	private final IDistribution UP = new InvTransform(UP_K, UP_ALPHA);
	private final IDistribution DOWN = new InvTransform(DOWN_K, DOWN_ALPHA);

	private final Random fRandom;

	public LeskovecHorvitz(@Attribute("Random") Random random) {
		fRandom = random;
	}

	@Override
	public IDistribution uptimeDistribution(double li) {
		return UP;
	}

	@Override
	public IDistribution downtimeDistribution(double di) {
		return DOWN;
	}

	@Override
	public String id() {
		return "LeskovecHorvitz";
	}

	class InvTransform implements IDistribution {

		private final double k;

		private final double alpha;

		public InvTransform(double k, double alpha) {
			this.k = k;
			this.alpha = alpha;
		}

		@Override
		public double sample() {
			return transform(fRandom.nextDouble());
		}

		private double transform(double x) {
			return (Math.pow((1.0 - x) * k, alpha));
		}

		@Override
		public double expectation() {
			return Double.POSITIVE_INFINITY;
		}

	}
}
