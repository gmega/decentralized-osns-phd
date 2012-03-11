package it.unitn.disi.churn.connectivity;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Future;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.ITopKEstimator;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.graph.analysis.TopKShortest;
import it.unitn.disi.graph.analysis.TopKShortestDisjoint;
import it.unitn.disi.graph.analysis.TopKShortestDisjoint.Mode;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.exception.ParseException;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * "Script" putting together all temporal estimate experiments.
 * 
 * @author giuliano
 */
@AutoConfig
public class TemporalEstimateExperiment implements IMultiTransformer {

	@Attribute("etype")
	private String fModeStr;

	@Attribute("repetitions")
	private int fRepetitions;

	@Attribute("k")
	private int fK;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute(value = "printpaths", defaultValue = "false")
	private boolean fPrintPaths;

	@Attribute(value = "start", defaultValue = "0")
	private int fStart;

	@Attribute(value = "end", defaultValue = "-1")
	private int fEnd;

	@Attribute(value = "tpk", defaultValue = "yen")
	private String fEstimator;

	@Attribute(value = "edgesample", defaultValue = "false")
	private boolean fSampleActivations;
	
	@Attribute(value = "cores", defaultValue = "-1")
	private int fCores;

	// @Attribute(value = "pair", defaultValue = Attribute.VALUE_NULL)
	String fPair;

	private int fSampleId;

	private int fSource;

	private int fSize;

	private Experiment fMode;

	private ProgressTracker fTracker;

	private final CallbackThreadPoolExecutor<double[]> fExecutor;

	private GraphConfigurator fGraphConf;

	private YaoChurnConfigurator fYaoConf;

	static enum Experiment {
		all(1 | 2 | 4), simulate(1), estimate(2), kestimate(4);

		private int fMode;

		private Experiment(int mode) {
			fMode = mode;
		}

		public boolean should(Experiment exp) {
			return (fMode & exp.fMode) != 0;
		}
	};

	public static enum Inputs {
		assignments, weights
	}

	public static enum Outputs {
		estimates
	}

	private static final int LI = 0;
	private static final int DI = 1;

	public TemporalEstimateExperiment(
			@Attribute(Attribute.AUTO) IResolver resolver) {

		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);

		fExecutor = new CallbackThreadPoolExecutor<double[]>(Runtime
				.getRuntime().availableProcessors(),
				new IExecutorCallback<double[]>() {
					@Override
					public void taskFailed(Future<double[]> task, Exception ex) {
						ex.printStackTrace();
						fTracker.tick();
					}

					@Override
					public synchronized void taskDone(double[] result) {
						fTracker.tick();
					}
				});
	}

	@Override
	public void execute(StreamProvider provider) throws Exception {
		fMode = Experiment.valueOf(fModeStr.toLowerCase());

		IGraphProvider loader = fGraphConf.graphProvider();

		TableReader assignments = new TableReader(
				provider.input(Inputs.assignments));

		TableReader weights = new TableReader(provider.input(Inputs.weights));

		ArrayList<String> fields = new ArrayList<String>();
		fields.add("id");
		fields.add("source");
		fields.add("target");

		System.err.print(" -- Will:  ");
		if (fMode.should(Experiment.estimate)) {
			System.err.print(" estimate ");
			fields.add("estimate");
		}

		if (fMode.should(Experiment.kestimate)) {
			System.err.print(" k-estimate ");
			fields.add("kestimate");
		}

		if (fMode.should(Experiment.simulate)) {
			System.err.print(" simulate ");
			fields.add("simulation");
		}

		System.err.println(".");

		TableWriter result = new TableWriter(new PrefixedWriter("ES:",
				new OutputStreamWriter(provider.output(Outputs.estimates))),
				(String[]) fields.toArray(new String[fields.size()]));

		IndexedNeighborGraph graph = null;
		int[] ids = null;
		double[][] w = null;
		double[][] ld = null;

		assignments.next();
		weights.next();

		int count = 0;

		while (assignments.hasNext()) {
			int root = Integer.parseInt(assignments.get("id"));
			boolean skip = false;

			count++;
			if (count < fStart) {
				System.err.println("-- Skipping sample " + root + ".");
				skip = true;
			}

			if (fEnd > 0 && count >= fEnd) {
				System.err.println("-- Stopping at sample " + root + ".");
				break;
			}

			if (!skip) {
				graph = loader.subgraph(root);
				ids = loader.verticesOf(root);
			}

			w = readWeights(root, weights, graph, ids, skip);
			ld = readLiDi(root, assignments, graph, ids, skip);

			if (skip) {
				continue;
			}

			int[] pair = null;
			if (fPair != null) {
				pair = new int[2];
				pair[0] = Integer.parseInt(fPair.split(",")[0]);
				pair[1] = Integer.parseInt(fPair.split(",")[1]);
			}

			for (int i = 0; i < graph.size(); i++) {
				if (pair != null && pair[0] != ids[i]) {
					continue;
				}

				setSample(root, i, graph.size());

				double[] estimates = null;
				if (fMode.should(Experiment.estimate)) {
					estimates = estimate(graph, i, w, ids);
				}

				double[] kEstimates = null;
				if (fMode.should(Experiment.kestimate)) {
					kEstimates = kEstimate(graph, i, w, ld, fK, ids);
				}

				double[] simulation = null;
				if (fMode.should(Experiment.simulate)) {
					simulation = simulate(sampleString() + ", full", graph, i,
							ld, ids);
				}

				for (int j = 0; j < graph.size(); j++) {
					if (i == j || !isPair(i, j, ids)) {
						continue;
					}

					result.set("id", root);
					result.set("source", ids[i]);
					result.set("target", ids[j]);

					if (simulation != null) {
						result.set("simulation", simulation[j] / fRepetitions);
					}

					if (estimates != null) {
						result.set("estimate", estimates[j]);
					}

					if (kEstimates != null) {
						result.set("kestimate", kEstimates[j] / fRepetitions);
					}

					result.emmitRow();
				}
			}
		}

		fExecutor.shutdown();
	}

	private boolean isPair(int i, int j, int[] ids) {
		if (fPair == null) {
			return true;
		}

		String[] p = fPair.split(",");
		int[] pair = { Integer.parseInt(p[0]), Integer.parseInt(p[1]) };

		return pair[0] == ids[i] && pair[1] == ids[j];
	}

	private double[] kEstimate(IndexedNeighborGraph graph, int source,
			double[][] w, double[][] lds, int k, int[] ids) throws Exception {
		ITopKEstimator tpk = estimator(graph, w);

		double[] results = new double[graph.size()];

		// For each vertex pair (u, w)
		for (int i = 0; i < graph.size(); i++) {
			if (i == source || !isPair(source, i, ids)) {
				continue;
			}

			System.out.println("Source: " + source + "(" + ids[source]
					+ ") Target: " + i + "(" + ids[ids[i]]);

			// 1. computes the top-k shortest paths between u and w.
			int[] vertexes = vertexesOf(tpk.topKShortest(source, i, k));
			LightweightStaticGraph kPathGraph = LightweightStaticGraph
					.subgraph((LightweightStaticGraph) graph, vertexes);

			// 2. runs a connectivity simulation on the subgraph
			// composed by the top-k shortest paths.
			double ldSub[][] = new double[vertexes.length][2];
			for (int j = 0; j < ldSub.length; j++) {
				ldSub[j][0] = lds[vertexes[j]][0];
				ldSub[j][1] = lds[vertexes[j]][1];
			}

			int remappedSource = indexOf(source, vertexes);
			int remappedTarget = indexOf(i, vertexes);

			double[] estimate = simulate(sampleString() + ", k-paths (" + i
					+ "/" + graph.size() + ")", kPathGraph, remappedSource,
					ldSub, ids);
			results[i] = estimate[remappedTarget];
		}

		return results;
	}

	private ITopKEstimator estimator(IndexedNeighborGraph graph, double[][] w) {
		if (fEstimator.equals("yen")) {
			return new TopKShortest(graph, w);
		}

		else if (fEstimator.equals("vd")) {
			return new TopKShortestDisjoint(graph, w, Mode.VertexDisjoint);
		}

		else if (fEstimator.equals("ed")) {
			return new TopKShortestDisjoint(graph, w, Mode.EdgeDisjoint);
		}

		throw new IllegalArgumentException();
	}

	private int indexOf(int element, int[] vertexes) {
		for (int i = 0; i < vertexes.length; i++) {
			if (element == vertexes[i]) {
				return i;
			}
		}

		throw new NoSuchElementException();
	}

	private int[] vertexesOf(ArrayList<? extends PathEntry> topKShortest) {
		Set<Integer> vertexSet = new HashSet<Integer>();
		for (PathEntry entry : topKShortest) {
			if (fPrintPaths) {
				System.out.println(Arrays.toString(entry.path));
			}
			for (int i = 0; i < entry.path.length; i++) {
				vertexSet.add(entry.path[i]);
			}
		}

		int[] vertexes = new int[vertexSet.size()];
		int i = 0;
		for (Integer element : vertexSet) {
			vertexes[i++] = element;
		}

		Arrays.sort(vertexes);
		return vertexes;
	}

	private double[] estimate(IndexedNeighborGraph graph, int source,
			double[][] w, int[] ids) {

		System.err.println("Estimating for source " + source + ".");
		double[] minDists = new double[graph.size()];
		int[] previous = new int[graph.size()];
		Arrays.fill(previous, Integer.MAX_VALUE);

		GraphAlgorithms.dijkstra(graph, source, w, minDists, previous);

		for (int i = 1; i < graph.size(); i++) {
			int vertex = i;
			if (fPrintPaths) {
				while (previous[vertex] != Integer.MAX_VALUE) {
					System.err.println(ids[vertex] + " <- "
							+ ids[previous[vertex]] + " ("
							+ w[previous[vertex]][vertex] + ")");
					vertex = previous[vertex];
				}
			}
		}

		return minDists;
	}

	private double[] simulate(String taskStr, IndexedNeighborGraph graph,
			int source, double[][] ld, int[] ids) throws Exception {

		ArrayList<Future<double[]>> tasks = new ArrayList<Future<double[]>>();
		double[] ttc = new double[graph.size()];

		ActivationSampler sampler = fSampleActivations ? new ActivationSampler(
				graph) : null;

		fTracker = Progress.newTracker(taskStr, fRepetitions);
		fTracker.startTask();
		for (int j = 0; j < fRepetitions; j++) {
			tasks.add(fExecutor.submit(new SimulationTask(ld, source, fBurnin,
					graph, sampler, fYaoConf)));
		}

		for (Future<double[]> task : tasks) {
			double[] tce = task.get();
			for (int i = 0; i < ttc.length; i++) {
				ttc[i] += tce[i];
			}
		}

		// Print activations.
		if (sampler != null) {
			sampler.printActivations(ids);
		}

		return ttc;
	}

	private void setSample(int sampleId, int sourceId, int totalSources) {
		fSampleId = sampleId;
		fSource = sourceId;
		fSize = totalSources;
	}

	private String sampleString() {
		return fSampleId + " (" + fSource + "/" + fSize + ")";
	}

	private double[][] readLiDi(int root, TableReader assignments,
			IndexedNeighborGraph graph, int[] ids, boolean skip)
			throws IOException {
		double[][] lidi = null;
		int size = skip ? Integer.MAX_VALUE : graph.size();

		if (!skip) {
			lidi = new double[graph.size()][2];
		}

		for (int i = 0; i < size; i++) {
			int id = Integer.parseInt(assignments.get("id"));

			if (root != id) {
				if (!skip) {
					throw new ParseException("ID doesn't match current root.");
				} else {
					break;
				}
			}

			if (!skip) {
				int node = idOf(Integer.parseInt(assignments.get("node")), ids);
				lidi[node][LI] = Double.parseDouble(assignments.get("li"));
				lidi[node][DI] = Double.parseDouble(assignments.get("di"));
			}
			assignments.next();
		}

		return lidi;
	}

	private int idOf(int id, int[] ids) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == id) {
				return i;
			}
		}

		throw new NoSuchElementException();
	}

	private double[][] readWeights(int root, TableReader weights,
			IndexedNeighborGraph graph, int[] ids, boolean skip)
			throws IOException {

		double[][] w = null;

		if (!skip) {
			w = new double[graph.size()][graph.size()];
			for (int i = 0; i < w.length; i++) {
				Arrays.fill(w[i], Double.MAX_VALUE);
			}
		}

		for (int i = 0; weights.hasNext(); i++) {
			int id = Integer.parseInt(weights.get("id"));
			if (root != id) {
				break;
			}

			if (!skip) {
				int source = idOf(Integer.parseInt(weights.get("source")), ids);
				int target = idOf(Integer.parseInt(weights.get("target")), ids);
				double weight = Double.parseDouble(weights.get("ttc"));
				w[source][target] = weight;
			}

			weights.next();
		}

		return w;
	}
}
