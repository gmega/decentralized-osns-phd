package it.unitn.disi.churn.protocols;

import java.util.LinkedList;
import java.util.List;

import it.unitn.disi.churn.PredefinedDistribution;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.core.RenewalProcess;

import org.junit.Assert;
import org.junit.Test;

public class PeriodicActionTest {

	private List<Double> fActionTimes = new LinkedList<Double>();

	private List<Integer> fActionId = new LinkedList<Integer>();

	@Test
	public void testSingleAction() {
		IProcess process = new RenewalProcess(0, new PredefinedDistribution(
				new double[] { 1.01, 2.3, 500 }), State.down);

		EngineBuilder builder = new EngineBuilder();
		builder.setExtraPermits(1);

		FixedTimerAction action = new FixedTimerAction(builder.reference(), 0,
				0);
		builder.addProcess(process);
		process.addObserver(action);

		EDSimulationEngine engine = builder.engine();
		engine.step(10);

		actionTimes(0.5, 1.0, 3.31, 3.81);
	}

	@Test
	public void testMultipleAction() {
		IProcess process = new RenewalProcess(0, new PredefinedDistribution(
				new double[] { 1.01, 2.3, 500 }), State.down);

		EngineBuilder builder = new EngineBuilder();
		builder.setExtraPermits(1);

		FixedTimerAction a1 = new FixedTimerAction(builder.reference(), 0, 0);
		FixedTimerAction a2 = new FixedTimerAction(builder.reference(), 1, 0);
		FixedTimerAction a3 = new FixedTimerAction(builder.reference(), 2, 0);

		builder.addProcess(process);
		process.addObserver(a2);
		process.addObserver(a1);
		process.addObserver(a3);

		EDSimulationEngine engine = builder.engine();
		engine.step(20);

		actionTimes(0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 3.31, 3.31, 3.31,
				3.81, 3.81, 3.81);
		
		actionList(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2);
	}

	private void actionTimes(double... times) {
		for (int i = 0; i < times.length; i++) {
			Assert.assertEquals(times[i], (double) fActionTimes.get(i),
					0.00000001);
		}
	}
	
	private void actionList(int...actions) {
		for(int i = 0; i < actions.length; i++) {
			Assert.assertEquals(actions[i], (int) fActionId.get(i));
		}
	}

	@SuppressWarnings("serial")
	class FixedTimerAction extends PeriodicAction {

		public FixedTimerAction(IReference<ISimulationEngine> engine, int prio,
				int id) {
			super(engine, prio, id, 0.5);
		}

		@Override
		protected double nextCycle(ISimulationEngine engine) {
			return engine.clock().rawTime() + 0.5;
		}

		@Override
		protected void performAction(ISimulationEngine engine) {
			fActionTimes.add(engine.clock().rawTime());
			fActionId.add(getPriority());
		}

	}

}
