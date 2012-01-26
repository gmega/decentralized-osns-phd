package it.unitn.disi.churn.connectivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Properties;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.large.catalog.CatalogReader;
import it.unitn.disi.graph.large.catalog.CatalogRecordTypes;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.network.churn.yao.YaoPresets;
import it.unitn.disi.utils.exception.ParseException;
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
public class TemporalEstimateExperiment implements IMultiTransformer {

	@Attribute("graph")
	private String fGraphFile;

	@Attribute("catalog")
	private String fCatalogFile;

	@Attribute("yaomode")
	private String fMode;

	@Attribute("burnin")
	private double fBurnin;

	public static enum Inputs {
		assignments, weights
	}

	public static enum Outputs {
		estimates
	}

	private static final int LI = 0;
	private static final int DI = 1;

	@Override
	public void execute(StreamProvider provider) throws Exception {
		PartialLoader loader = new PartialLoader(new CatalogReader(
				new FileInputStream(new File(fCatalogFile)),
				CatalogRecordTypes.PROPERTY_RECORD), ByteGraphDecoder.class,
				new File(fGraphFile));
		loader.start(null);

		TableReader assignments = new TableReader(
				provider.input(Inputs.assignments));

		TableReader weights = new TableReader(provider.input(Inputs.weights));

		TableWriter result = new TableWriter(new PrintStream(
				provider.output(Outputs.estimates)), "id", "source", "target",
				"simulation", "estimate");

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
				double[] simulation = simulate(graph, i, ld);
				double[] estimates = estimate(graph, i, w);

				for (int j = 0; j < graph.size(); j++) {
					if (i == j) {
						continue;
					}
					result.set("id", root);
					result.set("source", ids[i]);
					result.set("target", ids[j]);
					result.set("simulation", simulation[j]);
					result.set("estimate", estimates[j]);
				}
			}
		}
	}

	private double[] estimate(IndexedNeighborGraph graph, int source,
			double[][] w) {
		double [] minDists = new double[graph.size()];
		double [] previous = new double[graph.size()];
		
		GraphAlgorithms.dijkstra(graph, source, w, minDists, previous);
		
		return minDists;
	}

	private double[] simulate(IndexedNeighborGraph graph, int source,
			double[][] ld) {
		RenewalProcess[] rp = new RenewalProcess[graph.size()];
		Configuration.setConfig(new Properties());
		IDistributionGenerator distGen = YaoPresets.mode(fMode.toUpperCase());

		for (int i = 0; i < rp.length; i++) {
			rp[i] = new RenewalProcess(i,
					distGen.uptimeDistribution(ld[i][LI]),
					distGen.downtimeDistribution(ld[i][DI]), State.down);
		}

		TemporalConnectivityExperiment tce = new TemporalConnectivityExperiment(
				graph, source);
		ArrayList<IChurnSim> sims = new ArrayList<IChurnSim>();
		sims.add(tce);
		
		ArrayList<Object> cookies = new ArrayList<Object>();
		cookies.add(new Object());
		
		BaseChurnSim bcs = new BaseChurnSim(rp, sims, cookies, fBurnin);
		bcs.run();
		
		double [] ttc = new double[graph.size()];
		
		for (int i = 0; i < graph.size(); i++) {
			ttc[i] = tce.reachTime(i);
		}
		
		return ttc;
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
