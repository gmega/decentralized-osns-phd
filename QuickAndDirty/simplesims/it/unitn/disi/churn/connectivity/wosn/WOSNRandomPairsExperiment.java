package it.unitn.disi.churn.connectivity.wosn;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Future;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.AssignmentReader;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.MatrixReader;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.connectivity.SimulationTask;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.ITopKEstimator;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.graph.analysis.TopKShortest;
import it.unitn.disi.graph.analysis.TopKShortestDisjoint;
import it.unitn.disi.graph.analysis.TopKShortestDisjoint.Mode;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class WOSNRandomPairsExperiment implements IMultiTransformer {

	public static enum Inputs {
		assignments, weights, pairs
	}

	public static enum Outputs {
		estimates
	}

	@Attribute("pairs")
	private int fPairs;

	@Attribute("k")
	private int fK;

	@Attribute("algorithm")
	private String fEstimator;

	@Attribute("repetitions")
	private int fRepetitions;

	@Attribute("burnin")
	double fBurnin;

	private YaoChurnConfigurator fYaoConf;

	private GraphConfigurator fGraphConf;

	private ProgressTracker fTracker;

	private CallbackThreadPoolExecutor<double[]> fExecutor;

	public WOSNRandomPairsExperiment(
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
						if (fTracker != null) {
							fTracker.tick();
						}
					}

					@Override
					public synchronized void taskDone(double[] result) {
						if (fTracker != null) {
							fTracker.tick();
						}
					}
				});

	}

	@Override
	public void execute(StreamProvider provider) throws Exception {
		IGraphProvider loader = fGraphConf.graphProvider();

		Random r = new Random();
		Neighborhood[] hoods = readPairs(provider.input(Inputs.pairs),
				provider.input(Inputs.assignments),
				provider.input(Inputs.weights), loader);

		TableWriter writer = new TableWriter(new PrefixedWriter("RS:",
				provider.output(Outputs.estimates)), "id", "source",
				"destination", "kestimate");

		int cNeighborhood = 0;
		for (int i = 0; i < fPairs; i++) {
			int id = -1;
			Pair<Integer, Integer> next = null;
			for (int j = 0; j < hoods.length; j++) {
				id = cNeighborhood % hoods.length;
				cNeighborhood++;
				next = hoods[id].draw(r);
				if (next != null) {
					System.err.println(" -- Draw pair: " + next.a + ", "
							+ next.b + ".");
					break;
				}
			}

			IndexedNeighborGraph ing = loader.subgraph(hoods[id].root);
			double result = kEstimate(ing, next, hoods[id].weights,
					hoods[id].liDis, fK, hoods[id].ids);

			writer.set("id", id);
			writer.set("source", next.a);
			writer.set("destination", next.b);
			writer.set("kestimate", result/fRepetitions);
			writer.emmitRow();
		}
	}

	private double kEstimate(IndexedNeighborGraph graph,
			Pair<Integer, Integer> pair, double[][] w, double[][] lds, int k,
			int[] ids) throws Exception {

		ITopKEstimator tpk = estimator(graph, w);

		int source = idOf(pair.a, ids);
		int target = idOf(pair.b, ids);

		// 1. computes the top-k shortest paths between u and w.
		int[] vertexes = vertexesOf(tpk.topKShortest(source, target, k));
		LightweightStaticGraph kPathGraph = LightweightStaticGraph.subgraph(
				(LightweightStaticGraph) graph, vertexes);

		// 2. runs a connectivity simulation on the subgraph
		// composed by the top-k shortest paths.
		double ldSub[][] = new double[vertexes.length][2];
		for (int j = 0; j < ldSub.length; j++) {
			ldSub[j][0] = lds[vertexes[j]][0];
			ldSub[j][1] = lds[vertexes[j]][1];
		}

		int remappedSource = indexOf(source, vertexes);
		int remappedTarget = indexOf(target, vertexes);

		double[] estimate = simulate("(" + pair.a + ", " + pair.b + ")",
				kPathGraph, remappedSource, ldSub, ids);

		return estimate[remappedTarget];
	}

	private double[] simulate(String taskStr, IndexedNeighborGraph graph,
			int source, double[][] ld, int[] ids) throws Exception {

		ArrayList<Future<double[]>> tasks = new ArrayList<Future<double[]>>();
		double[] ttc = new double[graph.size()];

		fTracker = Progress.newTracker(taskStr, fRepetitions);
		fTracker.startTask();
		for (int j = 0; j < fRepetitions; j++) {
			tasks.add(fExecutor.submit(new SimulationTask(ld, source, fBurnin,
					graph, null, fYaoConf)));
		}

		for (Future<double[]> task : tasks) {
			double[] tce = task.get();
			for (int i = 0; i < ttc.length; i++) {
				ttc[i] += tce[i];
			}
		}

		return ttc;
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

	private Neighborhood[] readPairs(InputStream pairs,
			InputStream assignments, InputStream weights,
			IGraphProvider provider) throws IOException, NumberFormatException {

		HashMap<Integer, Neighborhood> neighborhoods = new HashMap<Integer, Neighborhood>();

		MatrixReader wReader = new MatrixReader(weights, "id", "source",
				"target", "ttc");
		AssignmentReader aReader = new AssignmentReader(assignments, "id");

		while (wReader.hasNext()) {
			int root = Integer.parseInt(wReader.currentRoot());
			int[] ids = provider.verticesOf(root);
			neighborhoods.put(root, new Neighborhood(root, aReader.read(ids),
					wReader.read(ids), ids));
		}

		TableReader reader = new TableReader(pairs);
		int pCount = 0;
		System.err.print("Reading pairs...");

		// will throw exception if file is empty.
		reader.next();

		while (reader.hasNext()) {
			int id = Integer.parseInt(reader.get("id"));
			Neighborhood neighborhood = neighborhoods.get(id);
			if (neighborhood == null) {
				throw new IllegalStateException("Unknown neighborhood id " + id
						+ ".");
			}
			pCount += neighborhood.add(reader);
		}
		System.err.println("read [" + pCount + "] pairs.");

		// Prunes away neighborhoods with no pairs.
		Iterator<Integer> it = neighborhoods.keySet().iterator();
		while (it.hasNext()) {
			if (neighborhoods.get(it.next()).pairCount() == 0) {
				it.remove();
			}
		}

		return neighborhoods.values().toArray(
				new Neighborhood[neighborhoods.size()]);
	}
	
	protected int idOf(int id, int[] ids) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == id) {
				return i;
			}
		}

		throw new NoSuchElementException();
	}

	static class Neighborhood {

		public double[][] liDis;

		public double[][] weights;
		
		public final int root;

		public int[] ids;

		private TIntArrayList fSource;

		private TIntArrayList fDestination;

		private BitSet fUsed;

		public Neighborhood(int id, double[][] assignments, double[][] weights,
				int[] ids) {
			root = id;
			this.weights = weights;
			this.liDis = assignments;
			this.ids = ids;
			fSource = new TIntArrayList();
			fDestination = new TIntArrayList();
			fUsed = new BitSet();
		}

		public int pairCount() {
			return fSource.size() - fUsed.cardinality();
		}

		public int add(TableReader reader) throws IOException {
			int added = 0;
			do {
				int id = Integer.parseInt(reader.get("id"));
				if (id != root) {
					break;
				}

				fSource.add(Integer.parseInt(reader.get("source")));
				fDestination.add(Integer.parseInt(reader.get("target")));
				added++;

				reader.next();
			} while (reader.hasNext());

			return added;
		}

		public Pair<Integer, Integer> draw(Random r) {
			int draw = r.nextInt(pairCount());
			for (int i = fUsed.nextClearBit(0); i >= 0; i = fUsed
					.nextClearBit(i + 1)) {
				if (draw == 0) {
					fUsed.set(i);
					return new Pair<Integer, Integer>(fSource.get(i),
							fDestination.get(i));
				}
				draw--;
			}

			// Ops, nothing to draw...
			return null;
		}
	}
}
