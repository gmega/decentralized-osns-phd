package it.unitn.disi.churn.connectivity;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import junit.framework.Assert;

import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.IProcess.State;
import it.unitn.disi.churn.simulator.SimpleEDSim;
import it.unitn.disi.churn.simulator.IEventObserver;
import it.unitn.disi.churn.simulator.RenewalProcess;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.utils.collections.Pair;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class TestTCExperiment {

	@Test
	public void testQueue() {
		LinkedList<Integer> refQueue = new LinkedList<Integer>();
		BFSQueue bQueue = new BFSQueue(7);

		Random r = new Random();

		int elements = 0;

		for (int i = 0; i < 1000000; i++) {
			boolean add = (r.nextDouble() < 0.5);
			if (elements == bQueue.capacity()) {
				add = false;
			}

			if (elements == 0) {
				Assert.assertTrue(bQueue.isEmpty());
				add = true;
			}

			if (add) {
				Integer element = r.nextInt();
				bQueue.addLast(element);
				refQueue.addLast(element);
				elements++;
			} else {
				int element = refQueue.removeFirst();
				Assert.assertEquals((int) element, (int) bQueue.removeFirst());
				elements--;
			}
		}
	}

	@Test
	public void testReachability() {
		IndexedNeighborGraph graph = LightweightStaticGraph
				.fromAdjacency(new int[][] { 
						{ 1 }, 
						{ 0, 2 }, 
						{ 1, 3, 5 },
						{ 2, 4 }, 
						{ 3 }, 
						{ 2, 6 }, 
						{ 5 } 
				});

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

		TemporalConnectivityEstimator tce = new TemporalConnectivityEstimator(
				graph, 0);

		ArrayList<Pair<Integer, ? extends IEventObserver>> sims = new ArrayList<Pair<Integer, ? extends IEventObserver>>();
		sims.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, tce));

		SimpleEDSim bcs = new SimpleEDSim(processes, sims, 0);
		bcs.run();

		Assert.assertEquals(0.0, tce.reachTime(0));
		Assert.assertEquals(0.8, tce.reachTime(1));
		Assert.assertTrue(Math.abs(1.8 - tce.reachTime(2)) < 1e-5);
		Assert.assertEquals(2.9, tce.reachTime(3));
		Assert.assertEquals(3.8, tce.reachTime(4));
		Assert.assertTrue(Math.abs(4.8 - tce.reachTime(5)) < 1e-5);
		Assert.assertTrue(Math.abs(5.8 - tce.reachTime(6)) < 1e-5);
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
