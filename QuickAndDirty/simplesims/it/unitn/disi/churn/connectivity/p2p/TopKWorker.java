package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.algorithms.PathEntry;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Triplet;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;
import it.unitn.disi.utils.tabular.minidb.IndexedReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.NoSuchElementException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

import com.google.common.collect.ObjectArrays;

@AutoConfig
public class TopKWorker extends AbstractWorker {

	private static final int YEN = 1;
	private static final int EDGE_DISJOINT = 2;
	private static final int VERTEX_DISJOINT = 4;

	@Attribute("weights")
	private String fWeightDb;

	@Attribute("weight-index")
	private String fWeightIdx;

	@Attribute("k")
	private int fK;

	@Attribute("mode")
	private int fMode;

	public TopKWorker(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("mode") String mode) throws IOException {
		super(resolver, "id");
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		IndexedReader reader = IndexedReader.createReader(new File(fWeightIdx),
				new File(fWeightDb));

		MatrixReader wReader = new MatrixReader(reader.getReader(), "id",
				"source", "target", "delay");

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				writerFields());

		IScheduleIterator iterator = iterator();
		Integer row;
		while ((row = (Integer) iterator.nextIfAvailable()) != IScheduleIterator.DONE) {
			// Reads availabilities.
			Experiment exp = experimentReader().readExperimentByRow(row, provider());
			IndexedNeighborGraph graph = provider().subgraph(exp.root);
			int[] ids = provider().verticesOf(exp.root);

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

			writer.set("id", exp.root);
			writer.set("source", source);
			writer.set("target", target);
			writer.set("vertex", graph.size());
			writer.set("edge", ((LightweightStaticGraph) graph).edgeCount());

			if (isSelected(YEN)) {
				Triplet<IndexedNeighborGraph, PathEntry[], Double> result = simHelper()
						.topKEstimate(ts + " Yen", graph,
								TEExperimentHelper.YENS, exp.root, rSource,
								rTarget, w, exp.lis, exp.dis, fK, ids);
				writer.set("kyenttc", result.c);
				writer.set("kyenvertex", result.a.size());
				writer.set("yenpaths", result.b.length);
				writer.set("kyenedge",
						((LightweightStaticGraph) result.a).edgeCount());
			}

			if (isSelected(EDGE_DISJOINT)) {
				Triplet<IndexedNeighborGraph, PathEntry[], Double> result = simHelper()
						.topKEstimate(ts + " ED", graph,
								TEExperimentHelper.EDGE_DISJOINT, exp.root,
								rSource, rTarget, w, exp.lis, exp.dis, fK, ids);
				writer.set("kedttc", result.c);
				writer.set("kedvertex", result.a.size());
				writer.set("edpaths", result.b.length);
				writer.set("kededge",
						((LightweightStaticGraph) result.a).edgeCount());

			}

			if (isSelected(VERTEX_DISJOINT)) {
				Triplet<IndexedNeighborGraph, PathEntry[], Double> result = simHelper()
						.topKEstimate(ts + " ED", graph,
								TEExperimentHelper.VERTEX_DISJOINT, exp.root,
								rSource, rTarget, w, exp.lis, exp.dis, fK, ids);
				writer.set("kvdttc", result.c);
				writer.set("kvdvertex", result.a.size());
				writer.set("vdpaths", result.b.length);
				writer.set("kvdedge",
						((LightweightStaticGraph) result.a).edgeCount());
			}

			writer.emmitRow();
		}
	}

	private String[] writerFields() {
		String[] fields = { "id", "source", "target", "vertex", "edge" };
		if (isSelected(EDGE_DISJOINT)) {
			fields = ObjectArrays.concat(fields, new String[] { "kedttc",
					"kedvertex", "kededge", "edpaths" }, String.class);
		}

		if (isSelected(YEN)) {
			fields = ObjectArrays.concat(fields, new String[] { "kyenttc",
					"kyenvertex", "kyenedge", "yenpaths" }, String.class);
		}

		if (isSelected(VERTEX_DISJOINT)) {
			fields = ObjectArrays.concat(fields, new String[] { "kvdttc",
					"kvdvertex", "kvdedge", "vdpaths" }, String.class);
		}

		return fields;
	}

	private boolean isSelected(int mask) {
		return (fMode & mask) != 0;
	}
}
