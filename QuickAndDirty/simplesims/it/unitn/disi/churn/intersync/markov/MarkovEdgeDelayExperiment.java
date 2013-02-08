package it.unitn.disi.churn.intersync.markov;

import java.util.ArrayList;
import java.util.List;

import it.unitn.disi.churn.intersync.EdgeDelaySampler;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.measure.IncrementalStatsAdapter;
import it.unitn.disi.simulator.random.Exponential;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.MTUnitUniformDistribution;
import it.unitn.disi.simulator.random.MersenneTwister;
import it.unitn.disi.utils.collections.Pair;
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

	@Attribute("lambda_u")
	private double fLambdaUp;

	@Attribute("lambda_d")
	private double fLambdaDown;

	@Override
	public void run() {
		MersenneTwister mt = new MersenneTwister();
		IDistribution uniform = new MTUnitUniformDistribution(mt);

		System.out.println("model simulation");

		for (int i = 0; i < fExperiments; i++) {
			double lu = mt.nextDouble() * fLambdaUp;
			double lv = mt.nextDouble() * fLambdaUp;
			double du = mt.nextDouble() * fLambdaDown;
			double dv = mt.nextDouble() * fLambdaDown;

			IProcess[] processes = new IProcess[] {
					new RenewalProcess(0, new Exponential(lu, uniform),
							new Exponential(du, uniform), State.down),
					new RenewalProcess(1, new Exponential(lv, uniform),
							new Exponential(dv, uniform), State.down) };

			EDSimulationEngine engine = new EDSimulationEngine(processes, 0.0,
					0);
			List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
			IncrementalStats stats = new IncrementalStats();
			IncrementalStatsAdapter observer = new IncrementalStatsAdapter(
					stats);
			EdgeDelaySampler eds = new EdgeDelaySampler(engine, fN, false,
					observer);
			observers.add(new Pair<Integer, IEventObserver>(
					IProcess.PROCESS_SCHEDULABLE_TYPE, eds));
			engine.setEventObservers(observers);
			engine.run();

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
