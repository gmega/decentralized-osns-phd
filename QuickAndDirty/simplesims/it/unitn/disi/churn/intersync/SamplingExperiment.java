package it.unitn.disi.churn.intersync;

import gnu.trove.list.array.TDoubleArrayList;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.churnmodel.yao.YaoPresets.IDistributionGenerator;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.measure.IValueObserver;
import it.unitn.disi.simulator.random.GeneralizedPareto;
import it.unitn.disi.simulator.random.UniformDistribution;
import it.unitn.disi.utils.collections.Pair;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

@AutoConfig
public class SamplingExperiment implements ITransformer {

	private static enum Mode {
		pareto, trueavg, burninavg, genandsample
	};

	@Attribute
	boolean cloud;

	@Attribute
	int repetitions;

	@Attribute(value = "burnin", defaultValue = "0.0")
	double burnin;

	@Attribute(value = "alpha", defaultValue = "3")
	double fAlpha;

	@Attribute(value = "beta", defaultValue = "2")
	double fBeta;

	@Attribute(value = "samples", defaultValue = "0")
	private int fSamples;

	private Mode fMode;

	public SamplingExperiment(@Attribute("experiment") String experiment) {
		fMode = Mode.valueOf(experiment.toLowerCase());
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		switch (fMode) {

		case pareto:
			paretoSamples();
			break;

		case trueavg:
			simSamples(simEngine(burnin));
			break;

		case burninavg:
			if (burnin == 0.0) {
				throw new IllegalArgumentException("Burn-in can't be zero.");
			}
			burninSamples(simEngine(burnin));
			break;

		case genandsample:
			genAndSample(simEngine(burnin));
			break;
		}

	}

	private void paretoSamples() {
		GeneralizedPareto gp = new GeneralizedPareto(fAlpha, fBeta, 0,
				new UniformDistribution(new Random()));
		IncrementalStats stats = new IncrementalStats();

		for (int i = 0; i < repetitions; i++) {
			stats.add(gp.sample());
			System.out.println(stats.getAverage());
		}
	}

	private void simSamples(EDSimulationEngine engine) {
		System.err.println("No burn-in.");
		final IncrementalStats stats = new IncrementalStats();
		EdgeDelaySampler sexp = new EdgeDelaySampler(repetitions, cloud,
				new IValueObserver() {
					@Override
					public void observe(double value, ISimulationEngine engine) {
						stats.add(value);
						System.out.println(stats.getAverage());
					}

				});

		runSim(0.0, sexp);
	}

	private void burninSamples(EDSimulationEngine engine) {
		final IncrementalStats stats = new IncrementalStats();
		IValueObserver obs = new IValueObserver() {
			@Override
			public void observe(double value, ISimulationEngine engine) {
				stats.add(value);
				System.out.println(stats.getAverage());
			}
		};

		for (int i = 0; i < repetitions; i++) {
			EdgeDelaySampler sexp = new EdgeDelaySampler(1, cloud, obs);
			runSim(burnin, sexp);
		}
	}

	private void genAndSample(EDSimulationEngine engine) {
		final TDoubleArrayList p1All = new TDoubleArrayList();
		final TDoubleArrayList p2All = new TDoubleArrayList();

		System.err.print("Generating...");
		EdgeDelaySampler sexp = new EdgeDelaySampler(repetitions, cloud,
				IValueObserver.NULL_OBSERVER) {
			@Override
			protected void register(double p2Login, TDoubleArrayList p1Logins,
					ISimulationEngine engine) {
				super.register(p2Login, p1Logins, engine);
				p1All.addAll(p1Logins);
				p2All.add(p2Login);
			}
		};

		runSim(0.0, sexp);

		System.err.println("done.");

		IncrementalStats stats = new IncrementalStats();
		p1All.shuffle(new Random());

		// Now samples.
		for (int i = 0; i < fSamples; i++) {
			double p1Login = p1All.getQuick(i);

			// Finds the next login for p2.
			int p2LoginIdx = p2All.binarySearch(p1Login);
			if (p2LoginIdx < 0) {
				p2LoginIdx = (p2LoginIdx + 1) * (-1);
				while (p2All.get(p2LoginIdx) < p1Login) {
					p2LoginIdx++;
				}
			}

			stats.add(p2All.get(p2LoginIdx) - p1Login);
			System.out.println(stats.getAverage());
		}

	}

	private void runSim(double burnin, IEventObserver sim) {
		EDSimulationEngine churnSim = simEngine(burnin);

		ArrayList<Pair<Integer, ? extends IEventObserver>> sims = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		sims.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, sim));

		churnSim.setEventObservers(sims);

		churnSim.run();
	}

	private EDSimulationEngine simEngine(double burnin) {
		double[] li = { 0.022671232013507403, 0.15811117888599902 };
		double[] di = { 0.060429334420225356, 5.098033173656655 };

		YaoChurnConfigurator conf = new YaoChurnConfigurator("H", "yao");
		IDistributionGenerator distGen = conf.distributionGenerator();

		RenewalProcess pI = new RenewalProcess(0,
				distGen.uptimeDistribution(li[0]),
				distGen.downtimeDistribution(di[0]), State.down);

		RenewalProcess pJ = new RenewalProcess(1,
				distGen.uptimeDistribution(li[1]),
				distGen.downtimeDistribution(di[1]), State.down);

		EDSimulationEngine churnSim = new EDSimulationEngine(
				new RenewalProcess[] { pI, pJ }, burnin);
		return churnSim;
	}
}
