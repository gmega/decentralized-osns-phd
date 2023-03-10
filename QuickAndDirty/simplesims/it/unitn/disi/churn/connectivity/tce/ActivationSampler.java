package it.unitn.disi.churn.connectivity.tce;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.OutputStreamWriter;

public class ActivationSampler {

	private final ISimulationEngine fEngine;
	
	private final IndexedNeighborGraph fGraph;

	private int[][] fActivationCount;

	public ActivationSampler(ISimulationEngine engine,
			IndexedNeighborGraph graph) {
		synchronized (this) {
			fActivationCount = new int[graph.size()][graph.size()];
		}
		fEngine = engine;
		fGraph = graph;
	}

	public void reached(int u, CloudTCE estimator) {
		INetwork bcs = fEngine.network();
		for (int i = 0; i < fGraph.degree(u); i++) {
			int neighbor = fGraph.getNeighbor(u, i);
			// If neighbor is up and so are we, means we could've been reached
			// by the neighbor.
			if (bcs.process(neighbor).isUp() && estimator.isReached(neighbor)) {
				// Records reverse activation for efficiency.
				synchronized (this) {
					fActivationCount[u][neighbor]++;
				}
			}
		}
	}

	public synchronized int activationOf(int i, int j) {
		// Note to self: remember we do this because activations are recorded
		// for the inverse edge.
		return fActivationCount[j][i];
	}

	public void printActivations(int[] ids) {
		TableWriter writer = new TableWriter(new PrefixedWriter("ACT:",
				new OutputStreamWriter(System.out)), "source", "target",
				"activation");

		for (int i = 0; i < fGraph.size(); i++) {
			for (int j = 0; j < fGraph.size(); j++) {
				if (fGraph.isEdge(i, j)) {
					writer.set("source", ids[i]);
					writer.set("target", ids[j]);
					writer.set("activation", activationOf(i, j));
					writer.emmitRow();
				}
			}
		}
	}
}
