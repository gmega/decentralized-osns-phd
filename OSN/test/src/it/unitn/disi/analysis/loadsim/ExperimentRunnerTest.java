package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

public class ExperimentRunnerTest {
	@Test
	public void testRunner() throws Exception {

		UnitExperiment u1 = new UnitExperiment(0, 2, false);
		UnitExperiment u2 = new UnitExperiment(1, 1, false);
		UnitExperiment u3 = new UnitExperiment(2, 1, false);
		UnitExperiment u4 = new UnitExperiment(3, 1, false);

		for (int i = 0; i < 5; i++) {
			u1.addData(0, 1, 1);
			u1.addData(1, 1, 1);
			u1.addData(2, 1, 1);
			u3.addData(0, 1, 1);
			u3.addData(1, 1, 1);
		}

		u2.addData(0, 1, 1);
		u2.addData(2, 1, 1);
		u2.addData(0, 1, 1);
		u2.addData(2, 1, 1);

		SimpleScheduler scheduler = new SimpleScheduler(u1,
				new UnitExperiment[][] { { u1 }, { u4 }, { u2 }, { u3 }, {} });

		ExperimentRunner runner = new ExperimentRunner(u1, scheduler,
				new SimStub(u1, u2, u3, u4), true);
		Pair<Integer, Collection<? extends MessageStatistics>> a = runner
				.call();

		Assert.assertEquals((int) a.a, 0);

		MessageStatistics[] s = new MessageStatistics[3];
		for (MessageStatistics stats : a.b) {
			s[stats.id] = stats;
		}

		Assert.assertEquals(s[1].id, 1);
		Assert.assertEquals(7, s[1].received);
		Assert.assertEquals(7, s[1].sent);

		Assert.assertEquals(s[2].id, 2);
		Assert.assertEquals(7, s[2].received);
		Assert.assertEquals(7, s[2].sent);

		Assert.assertEquals(s[0].id, 0);
		Assert.assertEquals(9, s[0].sent);
		Assert.assertEquals(9, s[0].received);
	}
}

class SimStub implements ILoadSim {

	private UnitExperiment[] fExperiments;

	public SimStub(UnitExperiment... experiments) {
		fExperiments = experiments;
	}

	@Override
	public IndexedNeighborGraph getGraph() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void synchronizedPrint(String data) {
		System.err.println(data.toString());
	}

	@Override
	public UnitExperiment unitExperiment(int nodeId) {
		return fExperiments[nodeId];
	}

	@Override
	public boolean shouldPrintData(int experimentId, int participantId) {
		return true;
	}

}

class SimpleScheduler implements IScheduler {

	private UnitExperiment[][] fExperiments;

	private UnitExperiment fRoot;

	public SimpleScheduler(UnitExperiment root, UnitExperiment[][] schedule) {
		fExperiments = schedule;
		fRoot = root;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<UnitExperiment> atTime(int round) {
		UnitExperiment[] toSchedule = fExperiments[round];
		if (toSchedule.length == 0) {
			return Collections.EMPTY_LIST;
		}

		ArrayList<UnitExperiment> list = new ArrayList<UnitExperiment>();
		for (UnitExperiment experiment : toSchedule) {
			list.add(experiment);
		}
		return list;
	}

	@Override
	public boolean experimentDone(UnitExperiment experiment) {
		if (experiment == fRoot) {
			fRoot = null;
			return true;
		}
		return false;
	}

	@Override
	public boolean isOver() {
		return fRoot == null;
	}

}
