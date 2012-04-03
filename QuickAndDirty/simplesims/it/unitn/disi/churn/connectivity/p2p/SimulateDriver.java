package it.unitn.disi.churn.connectivity.p2p;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

import it.unitn.disi.churn.AssignmentReader;
import it.unitn.disi.churn.GraphConfigurator;
import it.unitn.disi.churn.connectivity.SimulationResults;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * Driver for brute-force sims with stacking of sources.
 * 
 * @author giuliano
 */
@AutoConfig
public class SimulateDriver extends TEDriver implements ITransformer {

	@Attribute(value = "stacking", defaultValue = "0")
	private int fStacking;

	@Attribute("start")
	private int fStart;

	@Attribute("end")
	private int fEnd;

	private final GraphConfigurator fConfig;

	public SimulateDriver(@Attribute(Attribute.AUTO) IResolver resolver) {
		super(resolver);

		fConfig = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		AssignmentReader assig = new AssignmentReader(is, "id");
		IGraphProvider provider = fConfig.graphProvider();

		TableWriter writer = new TableWriter(new PrintStream(oup), "id",
				"source", "target", "ttc");

		for (int count = 0; assig.hasNext() && count < fEnd; count++) {

			if (count < fStart) {
				System.err.println("-- Skipping sample " + count + " ("
						+ assig.currentRoot() + ").");
				assig.skipCurrent();
				continue;
			}

			int root = Integer.parseInt(assig.currentRoot());
			IndexedNeighborGraph graph = provider.subgraph(root);
			int ids[] = provider.verticesOf(root);
			double[][] lidi = assig.read(ids);

			int start = 0;
			int end;
			do {
				end = Math.min(graph.size() - 1, start + fStacking);
				SimulationResults[] results = helper().bruteForceSimulate(
						"(" + (start + 1) + " - " + (end + 1) + ")/"
								+ graph.size(), graph, start, end,
						lidi[AssignmentReader.LI], lidi[AssignmentReader.DI],
						ids, false, false);
				printResults(results, writer, root, ids);
				start = end + 1;
			} while (start < graph.size());
		}
	}

	private void printResults(SimulationResults[] results, TableWriter writer,
			int root, int[] ids) {
		for (SimulationResults result : results) {
			double[] ttc = result.bruteForce;
			double[] ttcCloud = result.cloud;
			for (int i = 0; i < ttc.length; i++) {
				writer.set("id", root);
				writer.set("source", ids[result.source]);
				writer.set("target", ids[i]);
				writer.set("ttc", ttc[i] / fRepetitions);
				writer.set("ttcloud", ttcCloud[i] / fRepetitions);
				writer.emmitRow();
			}
		}
	}

}
