package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.MatrixReader;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;

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

			simHelper().topKEstimate("", graph, exp.source, target, w, exp.lis,
					exp.dis, fK, exp.ids);
		}
	}

}
