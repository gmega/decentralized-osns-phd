package it.unitn.disi.analysis;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.Edge;
import it.unitn.disi.graph.GraphAlgorithms;
import it.unitn.disi.graph.GraphWriter;
import it.unitn.disi.graph.LightweightStaticGraph;
import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.utils.graph.codecs.ByteGraphDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import peersim.config.Attribute;
import peersim.graph.BitMatrixGraph;
import peersim.graph.Graph;

public class PercolationThresholdEstimator implements ITransformer {

	private final Random fRandom = new Random();
	
	private final peersim.graph.GraphAlgorithms fGa = new peersim.graph.GraphAlgorithms();
	
	private int fRuns;
	
	private int fMaxIterations;

	private double fTargetProbability = .9;
	
	private double fPrecision;

	public PercolationThresholdEstimator(
			@Attribute(value = "runs", defaultValue = "32") int runs, 
			@Attribute(value = "target_probab") double targetProbability,
			@Attribute(value = "epsilon", defaultValue = ".001") double precision, 
			@Attribute(value = "max_iterations", defaultValue = "100") int maxIterations) {
		
		fTargetProbability = targetProbability;
		fRuns = runs;
		fPrecision = precision;
		fMaxIterations = maxIterations;
	}

	public void execute(InputStream is, OutputStream oup) throws IOException {
		ByteGraphDecoder decoder = new ByteGraphDecoder(is);
		LightweightStaticGraph lsg = LightweightStaticGraph.load(decoder);
		ArrayList<Integer> vertexList = new ArrayList<Integer>();
		
		SubgraphDecorator neighborhood = new SubgraphDecorator(lsg, false);
		SubgraphDecorator component = new SubgraphDecorator(lsg, false);

		/**
		 * Estimates the minimum edge placement probability required for the 
		 * graph to be connected with a certain probability.
		 */
		for (int i = 0; i < lsg.size(); i++) {
			vertexList.clear();
			this.neighborhood(i, lsg, vertexList);
			neighborhood.setVertexList(vertexList);
			
			@SuppressWarnings("unchecked")
			Map<Integer, Integer> components = fGa.weaklyConnectedClusters(neighborhood);

			double p_c = 0.0;
			for (Integer root : components.keySet()) {
				List<Integer> componentVertices = componentSubgraph(i,
						neighborhood, component, root);
				p_c = percolationThresholdEstimate(component);
				GraphWriter.printAdjList(component, generateConnectedRandom(component, p_c),
						new OutputStreamWriter(oup));
				/**
				 * Now prints the edges and probabilities p_c. These probabilities
				 * are to be interpreted as: when the target of the edge is
				 * disseminating a message on behalf of the source of the edge, it
				 * should decide to forward the message to a shared neighbor with
				 * the source with probability p_c.
				 */
				for (Integer neighbor : componentVertices) {
					if (neighbor == i) {
						continue;
					}
					System.out.println(i + " " + neighbor + " " + p_c);
				}
			}
		}
	}

	/**
	 * Produces a subgraph composed by the core node and a connected set of its
	 * friends.
	 */
	private ArrayList<Integer> componentSubgraph(int node, SubgraphDecorator mapper, SubgraphDecorator component, int root) {
		ArrayList<Integer> vList = new ArrayList<Integer>();
		vList.add(node);
		for (int i = 0; i < mapper.size(); i++) {
			if (fGa.color[i] == root) {
				vList.add(mapper.inverseIdOf(i));
			}
		}
		
		component.setVertexList(vList);
		
		return vList;
	}
	
	private Graph generateConnectedRandom(Graph starting, double p_c) {
		Graph rnd_sub = null;
		do {
			 rnd_sub = randomSubgraph(starting, p_c, null);
		} while(!GraphAlgorithms.isConnected(rnd_sub));
		return rnd_sub;
	}

	public double percolationThresholdEstimate(Graph g) {
		double p_c = 0.0;
		for (int i = 0; i < fRuns; i++) {
			p_c += percolationThresholdExperiment(g);
		}

		return p_c / fRuns;
	}

	public double percolationThresholdExperiment(Graph g) {

		double lastValid = -1;
		double left = 0.0;
		double right = 1.0;

		for (int i = 0; i < fMaxIterations; i++) {
			double estimate = left + ((right - left) / 2.0);
			if (Math.abs(estimate - lastValid) <= fPrecision) {
				return estimate;
			}
			
			double probability = connectivityProbabilityOf(g, estimate);
			if (probability >= fTargetProbability) {
				right = lastValid = estimate;
			} else {
				left = estimate;
			}
			
		}

		throw new IllegalStateException(
				"Could not find the percolation threshold in the alloted iteration limit.");
	}
	
	private double connectivityProbabilityOf(Graph g, double p) {
		BitMatrixGraph vessel = new BitMatrixGraph(g.size());
		int connected = 0;
		
		for (int i = 0; i < fRuns; i++) {
			randomSubgraph(g, p, vessel);
			if (GraphAlgorithms.isConnected(vessel)) {
				connected++;
			} 
			cleanVessel(g, vessel);
		}
		
		return ((double) connected) / ((double) fRuns);
	}

	private void cleanVessel(Graph g, BitMatrixGraph vessel) {
		for (int i = 0; i < g.size(); i++) {
			for (Integer j : g.getNeighbours(i)) {
				vessel.clearEdge(i, j);
			}
		}
	}

	public BitMatrixGraph randomSubgraph(Graph g, double p,
			BitMatrixGraph vessel) {
		if (vessel == null) {
			vessel = new BitMatrixGraph(g.size(), false);
		}
		
		Set<Edge> edgeSet = new HashSet<Edge>(); 

		for (int i = 0; i < g.size(); i++) {
			for (Integer j : g.getNeighbours(i)) {
				edgeSet.add(new Edge(i, j, !g.directed()));
			}
		}
		
		for (Edge edge : edgeSet) {
			if (fRandom.nextDouble() < p) {
				vessel.setEdge(edge.source, edge.target);
			}
		}

		return vessel;
	}

	/**
	 * Returns a vertex list containing the neighborhood of a central vertex
	 * (excluding the central vertex itself).
	 */
	private void neighborhood(int i, LightweightStaticGraph lsg,
			ArrayList<Integer> vertexList) {
		int[] neighbors = lsg.fastGetNeighbours(i);
		for (Integer neighbor : neighbors) {
			vertexList.add(neighbor);
		}
	}
}
