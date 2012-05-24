package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.FixedProcess;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.simulator.IProcess.State;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.collections.Triplet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import peersim.graph.Graph;
import peersim.graph.GraphFactory;
import peersim.graph.NeighbourListGraph;

public class TestLiveGraphTransformer {

	@Test
	public void testMapping() {

		Random rnd = new Random();

		IndexedNeighborGraph random = LightweightStaticGraph
				.fromGraph(GraphFactory.wireKOut(new NeighbourListGraph(5000,
						false), 10, rnd));

		INetwork network = random(5000, rnd);

		SubgraphDecorator reference = new SubgraphDecorator(random, false);
		reference.setVertexList(collectLive(network));

		LiveTransformer lgt = new LiveTransformer();
		Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> result = lgt
				.live(random, network);

		Assert.assertEquals(result.b.size(), reference.size());

		// Now tests vertex-by-vertex.
		for (int i = 0; i < reference.size(); i++) {
			int originalId = reference.reverseMap(i);
			int testId = result.a.map(originalId);

			Set<Integer> neighborsRefr = collect(reference, i, reference);
			Set<Integer> neighborsTest = collect(result.c, testId, result.a);

			Assert.assertTrue(neighborsRefr.containsAll(neighborsTest));
			Assert.assertTrue(neighborsTest.containsAll(neighborsRefr));
		}

	}

	private Set<Integer> collect(Graph g, int id, IDMapper mapper) {
		Set<Integer> neis = new HashSet<Integer>();
		for (Integer element : g.getNeighbours(id)) {
			neis.add(mapper.reverseMap(element));
		}

		return neis;
	}

	private Collection<Integer> collectLive(INetwork network) {
		ArrayList<Integer> live = new ArrayList<Integer>();
		for (int i = 0; i < network.size(); i++) {
			if (network.process(i).isUp()) {
				live.add(i);
			}
		}
		return live;
	}

	private INetwork random(int size, Random rnd) {
		final ArrayList<IProcess> processes = new ArrayList<IProcess>();
		int alive = 0;
		for (int j = 0; j < size; j++) {
			State state = rnd.nextDouble() < 0.5 ? State.up : State.down;
			if (state == State.up) {
				alive++;
			}
			processes.add(new FixedProcess(j, state));
		}

		final int fAlive = alive;

		return new INetwork() {

			@Override
			public int size() {
				return processes.size();
			}

			@Override
			public IProcess process(int index) {
				return processes.get(index);
			}

			@Override
			public int live() {
				return fAlive;
			}

			@Override
			public double version() {
				return 0;
			}
		};
	}
}
