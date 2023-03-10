package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.diffusion.experiments.config.Utils;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.BitSet;
import java.util.List;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

/**
 * Worker for brute force simulations. Supports cloud nodes.
 * 
 * @author giuliano
 */
@AutoConfig
public class SimWorker extends AbstractWorker implements ITransformer {

	private static final int M1 = 0x1;
	private static final int M2 = 0x2;
	private static final int M3 = 0x4;

	/**
	 * Bitmap containing which nodes in which neighborhoods are cloud nodes.
	 */
	@Attribute(value = "cloudbitmap", defaultValue = "no")
	private String fCloudBitmapFile;

	@Attribute("cloudsims")
	private boolean fCloudSims;

	@Attribute(value = "printcloudnodes", defaultValue = "false")
	private boolean fPrintCloud;

	@Attribute(value = "monitorclusters", defaultValue = "false")
	private boolean fMonitorClusters;

	@Attribute(value = "skipmetrics", defaultValue = "false")
	private boolean fSkipMetrics;

	@Attribute(value = "simmode", defaultValue = "1")
	private int fSimMode;

	private BitSet fCloudBitmap;

	// -------------------------------------------------------------------------

	public SimWorker(@Attribute(Attribute.AUTO) IResolver resolver)
			throws IOException {
		super(resolver, "id");
	}

	// -------------------------------------------------------------------------

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "ed", "rd", "edcloud");

		TableWriter fastWriter = new TableWriter(
				new PrefixedWriter("ESF:", oup), "id", "source", "target",
				"ed", "rd", "edcloud");

		TableWriter adaptiveWriter = new TableWriter(new PrefixedWriter("ESA:",
				oup), "id", "source", "target", "ed", "rd", "edcloud");

		try {
			IScheduleIterator schedule = this.iterator();
			Integer row;
			while ((row = (Integer) schedule.nextIfAvailable()) != IScheduleIterator.DONE) {
				Experiment e = experimentReader().readExperimentByRow(row,
						provider());
				IndexedNeighborGraph graph = provider().subgraph(e.root);
				int[] ids = provider().verticesOf(e.root);
				int[] cloudNodes = cloudNodes(e, ids);

				printCloud(e.root, ids, cloudNodes);

				int source = MiscUtils.indexOf(ids,
						Integer.parseInt(source(e)));

				long startTime = System.nanoTime();
				List<? extends INodeMetric<?>> metric;

				if ((fSimMode & M1) != 0) {
					System.err.println("Burnin sim (normal) - " + row);

					metric = simHelper().bruteForceSimulate(e.toString(),
							graph, e.root, source, e.lis, e.dis, cloudNodes,
							fCloudSims, fMonitorClusters, false);
					System.err.println("PERF: O "
							+ (System.nanoTime() - startTime));

					printResults(e.root, source, metric, writer, ids);
				}

				if ((fSimMode & M2) != 0) {
					System.err.println("Burnin sim (adaptive) - " + row);

					startTime = System.nanoTime();
					metric = simHelper().bruteForceSimulate(e.toString(),
							graph, e.root, source, e.lis, e.dis, cloudNodes,
							fCloudSims, fMonitorClusters, true);
					System.err.println("PERF: OA "
							+ (System.nanoTime() - startTime));

					printResults(e.root, source, metric, adaptiveWriter, ids);
				}

				if ((fSimMode & M3) != 0) {
					System.err.println("Fast sim - " + row);
					startTime = System.nanoTime();
					metric = simHelper().bruteForceSimulateMulti(graph, e.root,
							source, e.lis, e.dis, null);
					System.err.println("PERF: N "
							+ (System.nanoTime() - startTime));

					printResults(e.root, source, metric, fastWriter, ids);
				}

			}
		} finally {
			shutdown();
		}
	}
	
	// -------------------------------------------------------------------------

	private String source(Experiment e) {
		String source = e.attributes.get("source");
		if (source == null) {
			source = e.attributes.get("node");
		}
		return source;
	}

	// -------------------------------------------------------------------------

	private void printCloud(int root, int[] ids, int[] cloudNodes) {
		if (!fPrintCloud) {
			return;
		}

		for (int i = 0; i < cloudNodes.length; i++) {
			StringBuffer sb = new StringBuffer();
			sb.append(root);
			sb.append(" ");
			sb.append(ids[cloudNodes[i]]);
			System.out.println(sb);
		}
	}

	// -------------------------------------------------------------------------

	private BitSet readCloudBitmap() throws Exception {

		BitSet cloudBmp;
		if (fCloudBitmapFile.equals("no")) {
			cloudBmp = new BitSet();
		} else {
			File cloudBitmapFile = new File(fCloudBitmapFile);
			System.err.println("-- Cloud bitmap is "
					+ cloudBitmapFile.getName() + ".");
			System.err.print("- Reading...");
			ObjectInputStream stream = null;

			try {
				stream = new ObjectInputStream(new FileInputStream(new File(
						fCloudBitmapFile)));
				cloudBmp = (BitSet) stream.readObject();
			} finally {
				MiscUtils.safeClose(stream, false);
			}
			System.err.println("done.");
		}

		System.err.println("Cloud nodes: " + cloudBmp.cardinality() + ".");
		return cloudBmp;
	}

	// -------------------------------------------------------------------------

	private int[] cloudNodes(Experiment exp, int[] ids) throws Exception {
		if (fCloudBitmap == null) {
			fCloudBitmap = readCloudBitmap();
		}
		
		if (fCloudBitmap.cardinality() > 0) {
			throw new UnsupportedOperationException("Reimplement!");	
		}
		
		return new int[0];
//
//		// Reads cloud nodes.
//		TIntArrayList cloud = new TIntArrayList();
//		for (int i = 0; i < ids.length; i++) {
//			int row = exp.entry.rowStart + i;
//			if (fCloudBitmap.get(row)) {
//				cloud.add(i);
//			}
//		}
//
//		return cloud.toArray();
	}

	// -------------------------------------------------------------------------

	private void printResults(int root, int source,
			List<? extends INodeMetric<?>> results, TableWriter writer,
			int[] ids) {

		if (fSkipMetrics) {
			return;
		}

		INodeMetric<Double> ed = Utils.lookup(results, "ed", Double.class);
		INodeMetric<Double> rd = Utils.lookup(results, "rd", Double.class);
		INodeMetric<Double> cloud = Utils.lookup(results, "cloud_delay",
				Double.class);

		for (int i = 0; i < ids.length; i++) {
			if (source == i) {
				continue;
			}
			writer.set("id", root);
			writer.set("source", ids[source]);
			writer.set("target", ids[i]);
			writer.set("rd", rd != null ? rd.getMetric(i) : 0);
			writer.set("edcloud", cloud != null ? cloud.getMetric(i) : 0);
			writer.set("ed", ed.getMetric(i));
			writer.emmitRow();
		}
	}

	// -------------------------------------------------------------------------

}
