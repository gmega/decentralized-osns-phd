package it.unitn.disi.churn.connectivity.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.util.BitSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.churn.connectivity.SimulationResults;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;

import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * Worker for brute force simulations. Supports cloud nodes.
 * 
 * @author giuliano
 */
@AutoConfig
public class SimWorker extends AbstractWorker implements ITransformer {

	/**
	 * Bitmap containing which nodes in which neighborhoods are cloud nodes.
	 */
	@Attribute(value = "cloudbitmap", defaultValue = Attribute.VALUE_NULL)
	private String fCloudBitmapFile;

	@Attribute("cloudsims")
	private boolean fCloudSims;

	private BitSet fCloudBitmap;

	public SimWorker(@Attribute(Attribute.AUTO) IResolver resolver)
			throws IOException {
		super(resolver, "id", "source");
	}

	// -------------------------------------------------------------------------

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "ttc", "pl", "ttcloud");

		try {
			IScheduleIterator schedule = this.iterator();
			Integer row;
			while ((row = (Integer) schedule.nextIfAvailable()) != IScheduleIterator.DONE) {
				Experiment e = readExperiment(row);
				IndexedNeighborGraph graph = provider().subgraph(e.root);
				int[] ids = provider().verticesOf(e.root);
				int[] cloudNodes = cloudNodes(e);

				SimulationResults results = simHelper().bruteForceSimulate(
						e.toString(), graph, e.source, e.lis, e.dis, ids,
						cloudNodes, false, fCloudSims);

				printResults(e.root, results, writer, ids);
			}
		} finally {
			shutdown();
		}
	}

	// -------------------------------------------------------------------------

	private BitSet readCloudBitmap() throws Exception {
		File cloudBitmapFile = new File(fCloudBitmapFile);
		System.err.println("-- Cloud bitmap is " + cloudBitmapFile.getName()
				+ ".");
		System.err.print("- Reading...");
		ObjectInputStream stream = new ObjectInputStream(new FileInputStream(
				new File(fCloudBitmapFile)));
		BitSet cloudbmp = (BitSet) stream.readObject();
		System.err
				.println("done. Cloud nodes: " + cloudbmp.cardinality() + ".");
		return cloudbmp;
	}

	// -------------------------------------------------------------------------

	private int[] cloudNodes(Experiment exp) throws Exception {
		if (fCloudBitmap == null) {
			fCloudBitmap = readCloudBitmap();
		}

		// Reads cloud nodes.
		TIntArrayList cloud = new TIntArrayList();
		for (int i = 0; i < exp.ids.length; i++) {
			int row = exp.entry.rowStart + i;
			if (fCloudBitmap.get(row)) {
				cloud.add(i);
			}
		}

		return cloud.toArray();

	}

	// -------------------------------------------------------------------------

	private void printResults(int root, SimulationResults results,
			TableWriter writer, int[] ids) {
		for (int i = 0; i < results.bruteForce.length; i++) {
			if (results.source == i) {
				continue;
			}
			writer.set("id", root);
			writer.set("source", ids[results.source]);
			writer.set("target", ids[i]);
			writer.set("ttc", results.bruteForce[i] / fRepeat);
			writer.set("ttcloud", results.cloud[i] / fRepeat);
			writer.set("pl", results.perceived[i] / fRepeat);
			writer.emmitRow();
		}
	}

	// -------------------------------------------------------------------------

}
