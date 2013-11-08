package it.unitn.disi.churn.intersync.markov;

import it.unitn.disi.churn.intersync.EdgeDelaySampler;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.measure.IncrementalStatsAdapter;
import it.unitn.disi.simulator.random.Exponential;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.MTUnitUniformDistribution;
import it.unitn.disi.simulator.random.MersenneTwister;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

/**
 * Experiment for validating the Markov model of average edge delays.
 * 
 * @author giuliano
 */
@AutoConfig
public class MarkovEdgeDelayExperiment implements Runnable {

	@Attribute("experiments")
	private int fExperiments;

	@Attribute("n")
	private int fN;

	@Override
	public void run() {
		MersenneTwister mt = new MersenneTwister();
		IDistribution uniform = new MTUnitUniformDistribution(mt);

		IAverageGenerator yao = YaoPresets
				.averageGenerator("yao", new Random());

		System.out.println("model simulation");

		for (int i = 0; i < fExperiments; i++) {
			double lu = 1.0 / yao.nextLI();
			double lv = 1.0 / yao.nextLI();
			double du = 1.0 / yao.nextDI();
			double dv = 1.0 / yao.nextDI();

			IProcess[] processes = new IProcess[] {
					new RenewalProcess(0, new Exponential(lu, uniform),
							new Exponential(du, uniform), State.down),
					new RenewalProcess(1, new Exponential(lv, uniform),
							new Exponential(dv, uniform), State.down) };

			EngineBuilder builder = new EngineBuilder();
			builder.addProcess(processes);

			IncrementalStats stats = new IncrementalStats();
			IncrementalStatsAdapter observer = new IncrementalStatsAdapter(
					stats);
			EdgeDelaySampler eds = new EdgeDelaySampler(fN, false, observer);
			builder.addObserver(eds, IProcess.PROCESS_SCHEDULABLE_TYPE, true,
					true);

			builder.engine().run();

			System.out.println((stableState(lv, dv) * firstHittingTime(lu, lv,
					du, dv)) + " " + stats.getAverage());
		}
	}

	private double firstHittingTime(double lu, double lv, double du, double dv) {
		return ((du + lu) * (du + dv + lv)) / (du * dv * (du + lu + dv + lv));
	}

	private double stableState(double lv, double dv) {
		return (lv) / (lv + dv);
	}
}
