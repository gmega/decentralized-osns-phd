package it.unitn.disi.churn.diffusion;

import java.util.BitSet;
import java.util.Random;

import it.unitn.disi.churn.simulator.FixedProcess;
import it.unitn.disi.churn.simulator.INetwork;
import it.unitn.disi.churn.simulator.IProcess;
import it.unitn.disi.churn.simulator.IProcess.State;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.IMultiCounter;
import it.unitn.disi.utils.SparseMultiCounter;

import org.junit.Test;

public class BiasedCentralitySelectorTest {
	
	@Test
	public void testSelector() {
		IndexedNeighborGraph graph = LightweightStaticGraph.fromAdjacency(
				new int[][] {
				{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, // 0
				{ 0, 2, 3, 4, 5, 6, 7, 8, 9, 10 }, // 1
				{ 0, 1, 3, 4, 5, 6, 7, 8, 9 }, // 2
				{ 0, 1, 2, 4, 5, 6, 7, 8 }, // 3
				{ 0, 1, 2, 3, 5, 6, 7 }, // 4
				{ 0, 1, 2, 3, 4, 6 }, // 5
				{ 0, 1, 2, 3, 4 }, // 6
				{ 0, 1, 2, 3 }, // 7
				{ 0, 1, 2 }, // 8
				{ 0, 1 }, // 9
				{ 0 } // 10
				});
		
		Random r = new Random(42);
		BiasedCentralitySelector slktor = new BiasedCentralitySelector(r, true);
		
		IMultiCounter<Integer> counter = new SparseMultiCounter<Integer>();
		BitSet bs = new BitSet();
		INetwork network = network(10);

		for (int i = 0; i < 20000; i++) {
			int selected = slktor.selectPeer(0, graph, bs, network);
			counter.increment(selected);
		}

		int last = Integer.MIN_VALUE;
		for (int i = 0; i < 10; i++) {
			int selections = counter.count(i);
			System.out.println("i: " +i + " "+ selections);
		//	Assert.assertTrue(selections > last);
			last = selections;
		}

	}

	private INetwork network(int size) {
		final IProcess p [] = new IProcess[size];
		for (int i = 0; i < p.length; i++) {
			p[i] = new FixedProcess(i, State.up);
		}
		
		return new INetwork() {
			
			@Override
			public int size() {
				return p.length;
			}
			
			@Override
			public IProcess process(int index) {
				return p[index];
			}
		};
	}
}
