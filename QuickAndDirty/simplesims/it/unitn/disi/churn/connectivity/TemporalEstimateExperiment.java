package it.unitn.disi.churn.connectivity;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.exception.ParseException;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * Reads a graph with churn assignments plus pairwise latency estimates, and
 * computes the shortest path.
 * 
 * 
 * @author giuliano
 */
@AutoConfig
public class TemporalEstimateExperiment extends YaoGraphExperiment implements
		IMultiTransformer {

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("repetitions")
	private int fRepetitions;

	private final CallbackThreadPoolExecutor<double[]> fExecutor;

	public static enum Inputs {
		assignments, weights
	}

	public static enum Outputs {
		estimates
	}

	private static final int LI = 0;
	private static final int DI = 1;

	public TemporalEstimateExperiment() {
		fExecutor = new CallbackThreadPoolExecutor<double[]>(Runtime
				.getRuntime().availableProcessors(),
				new IExecutorCallback<double[]>() {
					@Override
					public void taskFailed(Future<double[]> task, Exception ex) {
					}
					@Override
					public void taskDone(double[] result) {
					}
				});
	}

	@Override
	public void execute(StreamProvider provider) throws Exception {
		IGraphProvider loader = graphProvider();

		TableReader assignments = new TableReader(
				provider.input(Inputs.assignments));

		TableReader weights = new TableReader(provider.input(Inputs.weights));

		TableWriter result = new TableWriter(new PrefixedWriter("ES:",
				new OutputStreamWriter(provider.output(Outputs.estimates))),
				"id", "source", "target", "simulation", "estimate");

		IndexedNeighborGraph graph = null;
		int[] ids = null;
		double[][] w = null;
		double[][] ld = null;

		assignments.next();
		weights.next();

		while (assignments.hasNext()) {
			int root = Integer.parseInt(assignments.get("id"));

			graph = loader.subgraph(root);
			ids = loader.verticesOf(root);
			w = readWeights(root, weights, graph, ids);
			ld = readLiDi(root, assignments, graph, ids);

			for (int i = 0; i < graph.size(); i++) {
				double[] estimates = estimate(graph, i, w, ids);
				double[] simulation = simulate(graph, i, ld, ids);

				for (int j = 0; j < graph.size(); j++) {
					if (i == j) {
						continue;
					}
					result.set("id", root);
					result.set("source", ids[i]);
					result.set("target", ids[j]);
					result.set("simulation", simulation[j] / fRepetitions);
					result.set("estimate", estimates[j]);
					result.emmitRow();
				}
			}
		}
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
			while (previous[vertex] != Integer.MAX_VALUE) {
				System.err.println(ids[vertex] + " <- " + ids[previous[vertex]]
						+ " (" + w[previous[vertex]][vertex] + ")");
				vertex = previous[vertex];
			}
		}

		return minDists;
	}

	private double[] simulate(IndexedNeighborGraph graph, int source,
			double[][] ld, int[] ids) throws Exception {

		ArrayList<Future<double []>> tasks = new ArrayList<Future<double []>>(); 
		double[] ttc = new double[graph.size()];
		for (int j = 0; j < fRepetitions; j++) {
			tasks.add(fExecutor.submit(new SimulationTask(ld, source, graph)));
		}

		for (Future<double []> task : tasks) {
			double [] tce = task.get();
			for (int i = 0; i < ttc.length; i++) {
				ttc[i] += tce[i];
			} 
		}

		return ttc;
	}

	class SimulationTask implements Callable<double[]> {

		private final double[][] fld;

		private final int fSource;

		private final IndexedNeighborGraph fGraph;

		public SimulationTask(double[][] ld, int source,
				IndexedNeighborGraph graph) {
			super();
			this.fld = ld;
			this.fSource = source;
			this.fGraph = graph;
		}

		@Override
		public double[] call() throws Exception {

			RenewalProcess[] rp = new RenewalProcess[fGraph.size()];
			IDistributionGenerator distGen = distributionGenerator();

			for (int i = 0; i < rp.length; i++) {
				rp[i] = new RenewalProcess(i,
						distGen.uptimeDistribution(fld[i][LI]),
						distGen.downtimeDistribution(fld[i][DI]), State.down);
			}

			TemporalConnectivityExperiment tce = new TemporalConnectivityExperiment(
					fGraph, fSource);
			ArrayList<IChurnSim> sims = new ArrayList<IChurnSim>();
			sims.add(tce);

			ArrayList<Object> cookies = new ArrayList<Object>();
			cookies.add(new Object());

			BaseChurnSim bcs = new BaseChurnSim(rp, sims, cookies, fBurnin);
			bcs.run();

			double[] contrib = new double[fGraph.size()];
			for (int i = 0; i < contrib.length; i++) {
				contrib[i] = tce.reachTime(i);
			}
			return contrib;
		}

	}

	private double[][] readLiDi(int root, TableReader assignments,
			IndexedNeighborGraph graph, int[] ids) throws IOException {
		double[][] lidi = new double[graph.size()][2];

		for (int i = 0; i < graph.size(); i++) {
			int id = Integer.parseInt(assignments.get("id"));
			int node = idOf(Integer.parseInt(assignments.get("node")), ids);

			if (root != id) {
				throw new ParseException("ID doesn't match current root.");
			}

			lidi[node][LI] = Double.parseDouble(assignments.get("li"));
			lidi[node][DI] = Double.parseDouble(assignments.get("di"));
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
			IndexedNeighborGraph graph, int[] ids) throws IOException {
		double[][] w = new double[graph.size()][graph.size()];
		for (int i = 0; i < w.length; i++) {
			Arrays.fill(w[i], Double.MAX_VALUE);
		}

		for (int i = 0; weights.hasNext(); i++) {
			int id = Integer.parseInt(weights.get("id"));
			if (root != id) {
				break;
			}

			int source = idOf(Integer.parseInt(weights.get("source")), ids);
			int target = idOf(Integer.parseInt(weights.get("target")), ids);
			double weight = Double.parseDouble(weights.get("ttc"));

			w[source][target] = weight;
			weights.next();
		}

		return w;
	}
}
