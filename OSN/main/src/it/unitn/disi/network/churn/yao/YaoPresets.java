package it.unitn.disi.network.churn.yao;

import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.random.Exponential;
import it.unitn.disi.random.GeneralizedPareto;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.UniformDistribution;
import peersim.core.CommonState;

public class YaoPresets {
	/**
	 * Alpha parameters for the shifted Pareto distributions used to generate
	 * uptime and downtime averages.
	 */
	private static final double ALPHA = 3.0;

	private static final double BETA_UPTIME = 1.0;

	private static final double BETA_DOWNTIME = 2.0;
	
	// ------------------------------------------------------------------------
	// Preset average generators.
	// ------------------------------------------------------------------------

	private static final IDistribution fUniform = new UniformDistribution(CommonState.r);

	private static final IAverageGenerator YAO_GENERATOR = new AverageGeneratorImpl(
			new GeneralizedPareto(ALPHA, BETA_UPTIME, 0, fUniform),
			new GeneralizedPareto(ALPHA, BETA_DOWNTIME, 0, fUniform), "yao");

	private static final IAverageGenerator[] generators = new IAverageGenerator[] { YAO_GENERATOR };

	// ------------------------------------------------------------------------
	// Preset system modes.
	// ------------------------------------------------------------------------

	// -- Heavy tailed
	private static final IDistributionGenerator HEAVY_TAILED = new DualPareto(
			3.0, 3.0, 2.0, 2.0, 0, 0, "H", fUniform);

	// -- Very Heavy tailed
	private static final IDistributionGenerator VERY_HEAVY_TAILED = new DualPareto(
			1.5, 1.5, 2.0, 2.0, 0, 0, "VH", fUniform);

	// -- Exponential System
	private static final IDistributionGenerator EXPONENTIAL_SYSTEM = new IDistributionGenerator() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new Exponential(1.0 / li, fUniform);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new GeneralizedPareto(3.0, 2.0 * di, 0.0, fUniform);
		}

		@Override
		public String id() {
			return "E";
		}

	};

	private static final IDistributionGenerator[] modes = new IDistributionGenerator[] {
			HEAVY_TAILED, VERY_HEAVY_TAILED, EXPONENTIAL_SYSTEM };
	
	public static IAverageGenerator averageGenerator(String id) {
		for (IAverageGenerator generator : generators) {
			if (generator.id().toUpperCase().equals(id.toUpperCase())) {
				return generator;
			}
		}
		return null;
	}
	
	public static IDistributionGenerator mode(String id) {
		for (IDistributionGenerator mode : modes) {
			if (mode.id().toUpperCase().equals(id.toUpperCase())) {
				return mode;
			}
		}
		return null;
	}
}
