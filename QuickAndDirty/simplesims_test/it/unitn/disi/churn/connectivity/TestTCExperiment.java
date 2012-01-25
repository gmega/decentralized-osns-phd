package it.unitn.disi.churn.connectivity;

import java.util.ArrayList;

import junit.framework.Assert;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.random.IDistribution;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class TestTCExperiment {

	@Test
	public void testReachability() {
		IndexedNeighborGraph graph = LightweightStaticGraph
				.fromAdjacency(new int[][] { { 1 }, { 0, 2 }, { 1, 3, 5 },
						{ 2, 4 }, { 3 }, { 2, 6 }, { 5 } });

		ListMultimap<Integer, Double> intervals = ArrayListMultimap.create();

		addIntervals(0, intervals, 0.1, 1.0, 8.0);
		addIntervals(1, intervals, 0.9, 2.0, 8.0);
		addIntervals(2, intervals, 1.9, 5.0, 8.0);
		addIntervals(3, intervals, 3, 4.0, 8.0);
		addIntervals(4, intervals, 3.9, 4.0, 8.0);
		addIntervals(5, intervals, 4.9, 6.0, 8.0);
		addIntervals(6, intervals, 5.9, 6.0, 8.0);

		RenewalProcess[] processes = new RenewalProcess[7];

		for (int i = 0; i < processes.length; i++) {
			processes[i] = createProcess(i, State.up, intervals);
		}

		TemporalConnectivityExperiment tce = new TemporalConnectivityExperiment(
				graph, 0);

		ArrayList<IChurnSim> sims = new ArrayList<IChurnSim>();
		sims.add(tce);

		ArrayList<Object> cookies = new ArrayList<Object>();
		cookies.add(new Object());

		BaseChurnSim bcs = new BaseChurnSim(processes, sims, cookies, 0);
		bcs.run();
		
		Assert.assertEquals(0.1, tce.reachTime(0));
		Assert.assertEquals(0.9, tce.reachTime(1));
		Assert.assertEquals(1.9, tce.reachTime(2));
		Assert.assertEquals(3.0, tce.reachTime(3));
		Assert.assertEquals(3.9, tce.reachTime(4));
		Assert.assertEquals(4.9, tce.reachTime(5));
		Assert.assertEquals(5.9, tce.reachTime(6));
	}

	private void addIntervals(int id, Multimap<Integer, Double> intervals,
			double... events) {
		intervals.put(id, events[0]);
		for (int i = 0; i < (events.length - 1); i++) {
			intervals.put(id, events[i + 1] - events[i]);
		}
	}

	private RenewalProcess createProcess(int i, State initial,
			ListMultimap<Integer, Double> intervals) {
		IDistribution dist = eventDist(i, intervals);
		return new RenewalProcess(i, dist, dist, initial);
	}

	private IDistribution eventDist(final int i,
			final ListMultimap<Integer, Double> intervals) {

		return new IDistribution() {

			private int index;

			@Override
			public double sample() {
				double next = intervals.get(i).get(index);
				index++;
				return next;
			}

			@Override
			public double expectation() {
				return Double.NaN;
			}
		};
	}

}
