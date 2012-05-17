package it.unitn.disi.network.churn.yao;

import java.util.Random;

import org.lambda.functions.implementations.F1;

import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.simulator.random.Exponential;
import it.unitn.disi.simulator.random.GeneralizedPareto;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.UniformDistribution;
import peersim.core.CommonState;

@SuppressWarnings("unchecked")
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

	private static final IDistribution fUniform = new UniformDistribution(
			CommonState.r);

	private static final NamedF1<IDistribution, IAverageGenerator> YAO_GENERATOR = new NamedF1<IDistribution, IAverageGenerator>(
			"yao", null) {
		{
			ret(new AverageGeneratorImpl(new GeneralizedPareto(ALPHA,
					BETA_UPTIME, 0, a), new GeneralizedPareto(ALPHA,
					BETA_DOWNTIME, 0, a), "yao"));
		}
	};

	private static final NamedF1<IDistribution, IAverageGenerator>[] generators = new NamedF1[] { YAO_GENERATOR };

	// ------------------------------------------------------------------------
	// Preset system modes.
	// ------------------------------------------------------------------------

	// -- Heavy tailed
	private static final NamedF1<IDistribution, IDistributionGenerator> HEAVY_TAILED = new NamedF1<IDistribution, IDistributionGenerator>(
			"H", null) {
		{
			ret(new DualPareto(3.0, 3.0, 2.0, 2.0, 0, 0, "H", a));
		}
	};

	// -- Very Heavy tailed
	private static final NamedF1<IDistribution, IDistributionGenerator> VERY_HEAVY_TAILED = new NamedF1<IDistribution, IDistributionGenerator>(
			"VH", null) {
		{
			ret(new DualPareto(1.5, 1.5, 2.0, 2.0, 0, 0, "VH", fUniform));
		}
	};

	// -- Exponential System
	private static final NamedF1<IDistribution, IDistributionGenerator> EXPONENTIAL_SYSTEM = new NamedF1<IDistribution, IDistributionGenerator>(
			"E", null) {
		{
			ret(new IDistributionGenerator() {

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

			});
		}
	};

	private static final NamedF1<IDistribution, IDistributionGenerator>[] modes = new NamedF1[] {
			HEAVY_TAILED, VERY_HEAVY_TAILED, EXPONENTIAL_SYSTEM };

	public static IAverageGenerator averageGenerator(String id) {
		return averageGenerator(id, CommonState.r);
	}

	public static IAverageGenerator averageGenerator(String id, Random r) {
		for (NamedF1<IDistribution, IAverageGenerator> generator : generators) {
			if (generator.id().toUpperCase().equals(id.toUpperCase())) {
				return (IAverageGenerator) generator
						.call(new UniformDistribution(r));
			}
		}
		return null;
	}
	
	public static IDistributionGenerator mode(String id) {
		return mode(id, CommonState.r);
	}

	public static IDistributionGenerator mode(String id, Random r) {
		for (NamedF1<IDistribution, IDistributionGenerator> mode : modes) {
			if (mode.id().toUpperCase().equals(id.toUpperCase())) {
				return mode.call(new UniformDistribution(r));
			}
		}
		return null;
	}

	static class NamedF1<In, Out> extends F1<In, Out> {

		private final String fId;

		public NamedF1(String id, In a, Object... extraVariables) {
			super(a, extraVariables);
			fId = id;
		}

		public String id() {
			return fId;
		}

	}
}
