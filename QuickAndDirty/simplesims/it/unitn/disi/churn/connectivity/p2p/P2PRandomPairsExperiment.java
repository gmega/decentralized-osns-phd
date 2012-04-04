package it.unitn.disi.churn.connectivity.p2p;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.AssignmentReader;
import it.unitn.disi.churn.AssignmentReader.Assignment;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.MatrixReader;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class P2PRandomPairsExperiment implements IMultiTransformer {

	public static enum Inputs {
		assignments, weights, pairs
	}

	public static enum Outputs {
		estimates
	}

	@Attribute(value = "printStats", defaultValue = "false")
	boolean fPrintStats;

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

	private TEExperimentHelper fHelper;

	public P2PRandomPairsExperiment(
			@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);

		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);

		fHelper = new TEExperimentHelper(fYaoConf, fEstimator,
				TEExperimentHelper.ALL_CORES, fRepetitions, fBurnin);
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
				"destination", "kestimate", "kvertex", "vertex", "kedge",
				"edge");

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

			if (next == null) {
				System.err
						.println(" -- Ran out of pairs. Stopping simulation.");
			}

			LightweightStaticGraph ing = (LightweightStaticGraph) loader
					.subgraph(hoods[id].root);
			Pair<IndexedNeighborGraph, Double> result = fHelper.topKEstimate(
					"", ing, next.a, next.b, hoods[id].weights,
					hoods[id].assignment.li, hoods[id].assignment.di, fK,
					hoods[id].ids);

			writer.set("id", hoods[id].root);
			writer.set("source", next.a);
			writer.set("destination", next.b);
			writer.set("vertex", ing.size());
			writer.set("edge", ing.edgeCount());
			writer.set("kestimate", result.b / fRepetitions);
			writer.set("kvertex", result.a.size());
			writer.set("kedge", ((LightweightStaticGraph) result.a).edgeCount());
			writer.emmitRow();
		}
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

		Neighborhood[] array = neighborhoods.values().toArray(
				new Neighborhood[neighborhoods.size()]);

		if (fPrintStats) {
			// Prints statistics.
			int edgeCount = 0;
			int vCount = 0;
			System.out.println("ST:id cid vcount ecount");
			for (int i = 0; i < array.length; i++) {
				Neighborhood n = array[i];
				LightweightStaticGraph lsg = (LightweightStaticGraph) provider
						.subgraph(n.root);
				edgeCount += lsg.edgeCount();
				vCount += lsg.size();
				System.out.println("ST:" + i + " " + n.root + " " + lsg.size()
						+ " " + lsg.edgeCount());
			}
			System.out.println("TOT:" + vCount + " " + edgeCount);
			System.exit(0);
		}

		return array;
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

		public Assignment assignment;

		public double[][] weights;

		public final int root;

		public int[] ids;

		private TIntArrayList fSource;

		private TIntArrayList fDestination;

		private BitSet fUsed;

		public Neighborhood(int id, Assignment assignment, double[][] weights,
				int[] ids) {
			root = id;
			this.weights = weights;
			this.assignment = assignment;
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
			if (pairCount() <= 0) {
				return null;
			}
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
