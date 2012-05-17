package it.unitn.disi.churn.connectivity;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.OutputStreamWriter;

public class ActivationSampler {

	private final IndexedNeighborGraph fGraph;

	private int[][] fActivationCount;

	public ActivationSampler(IndexedNeighborGraph graph) {
		synchronized (this) {
			fActivationCount = new int[graph.size()][graph.size()];
		}
		fGraph = graph;
	}

	public void reached(int u, TemporalConnectivityEstimator estimator) {
		INetwork bcs = estimator.sim();
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

	public void printActivations(int [] ids) {
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
