package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.IndexedReader;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

@AutoConfig
public class TopKWorker extends AbstractWorker {

	@Attribute("weights")
	private String fWeightDb;

	@Attribute("weightidx")
	private String fWeightIdx;

	@Attribute("k")
	private int fK;

	public TopKWorker(@Attribute(Attribute.AUTO) IResolver resolver)
			throws IOException {
		super(resolver, "id");
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		IndexedReader reader = IndexedReader.createReader(new File(fWeightIdx),
				new File(fWeightDb));

		MatrixReader wReader = new MatrixReader(reader.getStream(), "id",
				"source", "target", "ttc");

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "kedttc", "kyenttc", "kedvertex",
				"kyenvertex", "vertex", "kededge", "kyenedge", "edge");

		IScheduleIterator iterator = iterator();
		Integer row;
		while ((row = (Integer) iterator.nextIfAvailable()) != IScheduleIterator.DONE) {
			// Reads availabilities.
			Experiment exp = experimentReader().readExperiment(row, provider());
			IndexedNeighborGraph graph = provider().subgraph(exp.root);
			int [] ids = provider().verticesOf(exp.root);
			
			int target = Integer.parseInt(exp.attributes.get("target"));
			int source = Integer.parseInt(exp.attributes.get("source"));

			int rTarget = MiscUtils.indexOf(ids, target);
			int rSource = MiscUtils.indexOf(ids, source);

			// Reads weights.
			if (reader.select(exp.root) == null) {
				throw new NoSuchElementException();
			}

			wReader.streamRepositioned();
			double[][] w = wReader.read(ids);
			
			String ts = "Job " + row;

			Pair<IndexedNeighborGraph, Double> resultED = simHelper()
					.topKEstimate(ts + " ED", graph, TEExperimentHelper.EDGE_DISJOINT,
							rSource, rTarget, w, exp.lis, exp.dis, fK,
							ids);

			Pair<IndexedNeighborGraph, Double> resultYen = simHelper()
					.topKEstimate(ts + " Yen", graph, TEExperimentHelper.YENS,
							rSource, rTarget, w, exp.lis, exp.dis, fK,
							ids);

			writer.set("id", exp.root);
			writer.set("source", source);
			writer.set("target", target);
			writer.set("kedttc", resultED.b / fRepeat);
			writer.set("kyenttc", resultYen.b / fRepeat);
			writer.set("kedvertex", resultED.a.size());
			writer.set("kyenvertex", resultYen.a.size());
			writer.set("vertex", graph.size());
			writer.set("kededge",
					((LightweightStaticGraph) resultED.a).edgeCount());
			writer.set("kyenedge",
					((LightweightStaticGraph) resultYen.a).edgeCount());
			writer.set("edge", ((LightweightStaticGraph) graph).edgeCount());
			writer.emmitRow();
		}
	}
}
