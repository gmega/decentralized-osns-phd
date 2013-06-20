package it.unitn.disi.simulator.protocol;

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
import it.unitn.disi.simulator.protocol.PeriodicAction;

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

		actionTimes(0.5, 0.5, 0.5, 1.0, 1.0, 1.0, 3.31, 3.31, 3.31, 3.81, 3.81,
				3.81);

		actionList(0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2, 0, 1, 2);
	}

	@Test
	public void testTimerChanges() {
		IProcess process = new RenewalProcess(0, new PredefinedDistribution(
				new double[] { 0.7, 2.4, 2.5, 5.0, 1.0, 1000.0 }), State.down);

		EngineBuilder builder = new EngineBuilder();
		builder.setExtraPermits(1);

		FixedTimerAction a1 = new FixedTimerAction(builder.reference(), 0, 0);
		builder.addProcess(process);
		process.addObserver(a1);

		EDSimulationEngine engine = builder.engine();

		Assert.assertFalse(a1.scheduled());

		// Process goes up, should schedule access.
		engine.step(1);
		Assert.assertTrue(a1.scheduled());

		// We now move the timer into the process' offline time.
		a1.newTimer(1.4);
		Assert.assertFalse(a1.scheduled());

		// Process goes offline, cleans the expired scheduled action.
		engine.step(2);
		Assert.assertFalse(a1.scheduled());

		// Process goes online.
		engine.step(1);
		Assert.assertTrue(a1.scheduled());

		// Action performed and rescheduled.
		engine.step(1);
		Assert.assertTrue(a1.scheduled());

		// Move the timer again.
		a1.newTimer(11.2);
		Assert.assertFalse(a1.scheduled());

		// Process goes offline, clean up expired scheduled action.
		engine.step(2);
		Assert.assertFalse(a1.scheduled());

		// Process goes online, actions gets scheduled again.
		engine.step(1);
		Assert.assertTrue(a1.scheduled());

		// Action performed, but not scheduled as there's not enough time.
		engine.step(1);
		Assert.assertFalse(a1.scheduled());

		actionTimes(3.1, 11.2);
	}

	@Test
	public void testNudgeAbort() {
		IProcess process = new RenewalProcess(0, new PredefinedDistribution(
				new double[] { 0.7, 0.5, PeriodicAction.TIEBREAK_DELTA / 2.0,
						0.1, 0.2, 1000.0 }), State.down);

		EngineBuilder builder = new EngineBuilder();
		builder.setExtraPermits(1);

		FixedTimerAction a1 = new FixedTimerAction(builder.reference(), 1, 0);
		builder.addProcess(process);
		process.addObserver(a1);

		EDSimulationEngine engine = builder.engine();

		Assert.assertFalse(a1.scheduled());

		// Process goes up, should schedule access.
		engine.step(1);
		Assert.assertTrue(a1.scheduled());

		// Action fires, should not schedule again as session is too short.
		engine.step(1);
		Assert.assertFalse(a1.scheduled());

		// Process goes offline.
		engine.step(1);
		Assert.assertFalse(process.isUp());

		// Process comes online, but can't schedule as session too short.
		engine.step(1);
		Assert.assertTrue(process.isUp());
		Assert.assertFalse(a1.scheduled());

		engine.step(3);

		actionTimes(0.5, 1.3);

	}

	private void actionTimes(double... times) {
		for (int i = 0; i < times.length; i++) {
			Assert.assertEquals(times[i], (double) fActionTimes.get(i),
					0.00000001);
		}
	}

	private void actionList(int... actions) {
		for (int i = 0; i < actions.length; i++) {
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
		protected double performAction(ISimulationEngine engine) {
			fActionTimes.add(engine.clock().rawTime());
			fActionId.add(getPriority());

			return engine.clock().rawTime() + 0.5;
		}

	}

}
