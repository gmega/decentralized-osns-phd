package it.unitn.disi.churn.intersync;

import java.util.ArrayList;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.random.Exponential;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.MTUnitUniformDistribution;
import it.unitn.disi.simulator.random.MersenneTwister;
import it.unitn.disi.utils.collections.Pair;

@AutoConfig
public class TimerOffsetEstimator implements Runnable {

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("timer")
	private double fTimer;

	@Attribute("lambda_u")
	private double fLambdaUp;

	@Attribute("lambda_d")
	private double fLambdaDown;

	private IncrementalStats fStats;

	@SuppressWarnings("serial")
	@Override
	public void run() {

		for (int i = 0; i < 100; i++) {
			
			MersenneTwister mt = new MersenneTwister();
			
			double u = mt.nextDouble()*fLambdaUp;
			double d = mt.nextDouble()*fLambdaDown;
			
			fStats = new IncrementalStats();

			IDistribution uniform = new MTUnitUniformDistribution(mt);
			for (int j = 0; j < 100000; j++) {
				EDSimulationEngine engine = new EDSimulationEngine(
						new IProcess[] { new RenewalProcess(0, new Exponential(
								u, uniform), new Exponential(
								d, uniform), State.down) }, fBurnin);

				engine.setEventObservers(new ArrayList<Pair<Integer, ? extends IEventObserver>>() {
					{
						add(new Pair<Integer, IEventObserver>(
								IProcess.PROCESS_SCHEDULABLE_TYPE,
								new Estimate()));
					}
				});

				engine.run();
			}

			double p4 = (u) / ((u + d) * d);

			System.out.println("V:" + fStats.getAverage()
					+ " " + p4);
		}
	}

	@Binding
	private class Estimate implements IEventObserver {

		private boolean fDone = false;

		@Override
		public void eventPerformed(ISimulationEngine engine,
				Schedulable schedulable, double nextShift) {

			if (engine.clock().time() < fTimer) {
				return;
			}

			IProcess process = (IProcess) schedulable;

			// We're above the timer.
			switch (process.state()) {

			// If we were down, the offset is the difference
			// between the current time (which we're going up)
			// and when the timer expired.
			case up:
				done(engine.clock().time() - fTimer, engine);
				break;

			// If we were up, the offset is zero.
			case down:
				done(0, engine);
				break;

			}

		}

		private void done(double i, ISimulationEngine engine) {
			fStats.add(i);
			// System.err.println("OFF:" + i);
			engine.unbound(this);
			fDone = true;
		}

		@Override
		public boolean isDone() {
			return fDone;
		}

	}
}
