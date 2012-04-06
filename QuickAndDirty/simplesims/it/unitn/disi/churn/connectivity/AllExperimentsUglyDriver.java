package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.YaoChurnConfigurator;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.utils.exception.ParseException;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

/**
 * "Script" putting together all temporal estimate experiments.
 * 
 * @author giuliano
 */
@AutoConfig
public class AllExperimentsUglyDriver implements IMultiTransformer {

	@Attribute("etype")
	private String fModeStr;

	@Attribute("repetitions")
	private int fRepetitions;

	@Attribute("k")
	private int fK;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute(value = "start", defaultValue = "0")
	private int fStart;

	@Attribute(value = "end", defaultValue = "-1")
	private int fEnd;

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

	private GraphConfigurator fGraphConf;

	private YaoChurnConfigurator fYaoConf;

	private TEExperimentHelper fHelper;

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

	public AllExperimentsUglyDriver(
			@Attribute(Attribute.AUTO) IResolver resolver) {

		fYaoConf = ObjectCreator.createInstance(YaoChurnConfigurator.class, "",
				resolver);
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
		fHelper = new TEExperimentHelper(fYaoConf, fCores, fRepetitions,
				fBurnin);
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
					estimates = fHelper.upperBound(graph, i, w);
				}

				double[] kEstimates = new double[graph.size()];
				if (fMode.should(Experiment.kestimate)) {
					for (int j = 0; j < graph.size(); j++) {
						if (j != i) {
							kEstimates[i] = fHelper.topKEstimate("top-k - " + i
									+ "/" + graph.size(), graph,
									TEExperimentHelper.EDGE_DISJOINT, i, j, w,
									ld[LI], ld[DI], fK, ids).b;
						}
					}
				}

				SimulationResults simulation = null;
				if (fMode.should(Experiment.simulate)) {
					simulation = fHelper.bruteForceSimulate(sampleString()
							+ ", full", graph, i, ld[LI], ld[DI], ids, null,
							fSampleActivations, false);
				}

				for (int j = 0; j < graph.size(); j++) {
					if (i == j || !isPair(i, j, ids)) {
						continue;
					}

					result.set("id", root);
					result.set("source", ids[i]);
					result.set("target", ids[j]);

					if (simulation != null) {
						result.set("simulation", simulation.bruteForce[j]
								/ fRepetitions);
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

		fHelper.shutdown(true);
	}

	private boolean isPair(int i, int j, int[] ids) {
		if (fPair == null) {
			return true;
		}

		String[] p = fPair.split(",");
		int[] pair = { Integer.parseInt(p[0]), Integer.parseInt(p[1]) };

		return pair[0] == ids[i] && pair[1] == ids[j];
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
