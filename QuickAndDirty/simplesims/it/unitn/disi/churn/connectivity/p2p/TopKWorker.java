package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.MatrixReader;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
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

	private IndexedReader fWeights;

	private int fK;

	public TopKWorker(@Attribute(Attribute.AUTO) IResolver resolver)
			throws IOException {
		super(resolver, "id", "source");
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		IndexedReader reader = IndexedReader.createReader(new File(fWeightIdx),
				new File(fWeightDb));

		MatrixReader wReader = new MatrixReader(reader.getStream(), "id",
				"source", "target", "ttc");

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "kttc", "kvertex", "vertex", "kedge",
				"edge");

		IScheduleIterator iterator = iterator();
		Integer row;
		while ((row = (Integer) iterator.nextIfAvailable()) != IScheduleIterator.DONE) {
			seekSourceRow(row);
			int root = Integer.parseInt(sourceReader().get("id"));
			int target = Integer.parseInt(sourceReader().get("target"));

			// Reads availabilities.
			Experiment exp = readExperiment(row);
			IndexedNeighborGraph graph = provider().subgraph(exp.root);

			// Reads weights.
			if (fWeights.select(root) == null) {
				throw new NoSuchElementException();
			}

			wReader.streamRepositioned();
			double[][] w = wReader.read(exp.ids);

			Pair<IndexedNeighborGraph, Double> resultED = simHelper()
					.topKEstimate("", graph, TEExperimentHelper.EDGE_DISJOINT,
							exp.source, target, w, exp.lis, exp.dis, fK,
							exp.ids);

			Pair<IndexedNeighborGraph, Double> resultYen = simHelper()
					.topKEstimate("", graph, TEExperimentHelper.YENS,
							exp.source, target, w, exp.lis, exp.dis, fK,
							exp.ids);

			writer.set("id", root);
			writer.set("source", exp.source);
			writer.set("target", target);
			writer.set("kedttc", resultED.b / fRepeat);
			writer.set("kyenttc", resultYen.b / fRepeat);
			writer.set("kvertex", resultED.a.size());
			writer.set("vertex", graph.size());
			writer.set("kedge",
					((LightweightStaticGraph) resultED.a).edgeCount());
			writer.set("edge", ((LightweightStaticGraph) graph).edgeCount());
			writer.emmitRow();
		}
	}
}
