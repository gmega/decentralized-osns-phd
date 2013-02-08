package it.unitn.disi.churn.intersync.markov;

import java.util.ArrayList;
import java.util.Arrays;

import jphase.ContPhaseVar;
import jphase.DenseContPhaseVar;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.DunnTopK;
import it.unitn.disi.graph.analysis.DunnTopK.Mode;
import it.unitn.disi.graph.analysis.PathEntry;

public class MarkovDelayModel {

	/**
	 * fMu is the array containing parameters for the session length exponential
	 * distributions.
	 */
	private final double[] fMu;

	/**
	 * fMu is the array containing parameters for the inter-session length
	 * exponential distributions.
	 */
	private final double[] fGamma;

	/**
	 * {@link DunnTopK} produces a set of <i>k</i> vertex-disjoint paths
	 * connecting two nodes.
	 */
	private final DunnTopK fEstimator;

	public MarkovDelayModel(IndexedNeighborGraph graph, double[] lambdaUp,
			double[] lambdaDown, int k) {
		this(graph, estimateWeights(graph, lambdaUp, lambdaDown), lambdaUp,
				lambdaDown);
	}

	public MarkovDelayModel(IndexedNeighborGraph graph, double[][] weights,
			double[] lambdaUp, double[] lambdaDown) {
		// Ideally we should copy these...
		fMu = lambdaUp;
		fGamma = lambdaDown;

		fEstimator = new DunnTopK(graph, weights, Mode.VertexDisjoint);
	}

	public double estimateDelay(int i, int j, int k) {
		ArrayList<PathEntry> paths = fEstimator.topKShortest(i, j, k);
		ArrayList<PhaseTypeDistribution> pathDelays = phaseVars(paths);
		return minimum(pathDelays).expectation();
	}

	private ArrayList<PhaseTypeDistribution> phaseVars(ArrayList<PathEntry> paths) {
		ArrayList<PhaseTypeDistribution> delays = new ArrayList<PhaseTypeDistribution>();
		for (PathEntry entry : paths) {
			PhaseTypeDistribution phaseVar = phaseVar(entry.path);
			delays.add(phaseVar);
		}

		return delays;
	}

	private PhaseTypeDistribution phaseVar(int[] path) {
		PhaseTypeDistribution pathDelay = null;
		for (int i = 0; i < (path.length - 1); i++) {

			PhaseTypeDistribution edgeDelay = new PhaseTypeDistribution(
					genMatrix(fMu[path[i]], fMu[path[i + 1]], fGamma[path[i]],
							fGamma[path[i + 1]]), alpha(fMu[path[i]],
							fMu[path[i + 1]], fGamma[path[i]],
							fGamma[path[i + 1]]));

			pathDelay = pathDelay == null ? edgeDelay : pathDelay
					.sum(edgeDelay);
			
		}

		return pathDelay;
	}

	private PhaseTypeDistribution minimum(ArrayList<PhaseTypeDistribution> pathDelays) {
		PhaseTypeDistribution minimum = pathDelays.get(0);
		for (int i = 1; i < pathDelays.size(); i++) {
			minimum = minimum.min(pathDelays.get(i));
		}
		return minimum;
	}

	public static double[][] genMatrix(double mu_u, double mu_v,
			double gamma_u, double gamma_v) {
		return new double[][] { 
				{ -(gamma_u + gamma_v),	gamma_v, 			gamma_u,			0 },
				{ mu_v, 				-(gamma_u + mu_v), 	0, 					gamma_u },
				{ mu_u, 				0, 					-(mu_u + gamma_v), 	gamma_v }, 
				{ 0,					0,					0,					0 } 
			};
	}

	public static double[] alpha(double mu_u, double mu_v, double gamma_u,
			double gamma_v) {

		// Alpha zero is the stable state probability of v being online.
		double pi_zero = mu_v / (mu_v + gamma_v);
		return new double[] { 0.0, 0.0, pi_zero, 1 - pi_zero };
	}

	public static double[][] estimateWeights(IndexedNeighborGraph graph,
			double[] lambdaUp, double[] lambdaDown) {
		double[][] weights = new double[graph.size()][graph.size()];
		for (int i = 0; i < weights.length; i++) {
			for (int j = 0; j < weights[i].length; j++) {
				weights[i][j] = graph.isEdge(i, j) ? edgeDelay(lambdaUp[i],
						lambdaUp[j], lambdaDown[i], lambdaDown[j])
						: Double.MAX_VALUE;
			}
		}
		return weights;
	}

	private static double edgeDelay(double lu, double lv, double du, double dv) {
		double firstHitting = ((du + lu) * (du + dv + lv))
				/ (du * dv * (du + lu + dv + lv));
		double stableState = (lv) / (lv + dv);
		return stableState * firstHitting;
	}
}
