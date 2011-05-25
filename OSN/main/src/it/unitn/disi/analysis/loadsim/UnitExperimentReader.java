package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.utils.TableReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import peersim.graph.Graph;

/**
 * Simple reader for {@link UnitExperiment}. Careful, as it <b>does not support
 * repetitions</b>.
 * 
 * @author giuliano
 */
public class UnitExperimentReader {
	/**
	 * Unit experiment row keys for {@link TableReader}.
	 */
	private static final String EXPERIMENT_ID = "root_id";
	private static final String NODE_ID = "neighbor_id";
	private static final String SENT = "sent";
	private static final String RECEIVED = "received";

	private final InputStream fInput;

	private final Graph fGraph;

	public UnitExperimentReader(InputStream input, Graph graph) {
		fInput = input;
		fGraph = graph;
	}
	
	public Map<Integer, UnitExperiment> load() throws IOException {
		return load(true, false);
	}

	public Map<Integer, UnitExperiment> load(boolean cumulative, boolean lastOnly) throws IOException {

		System.err.print("Reading unit experiment data ... ");

		Map<Integer, UnitExperiment> experiments = new HashMap<Integer, UnitExperiment>();

		// Now loads the actual data.
		TableReader reader = new TableReader(fInput);
		Integer currentRoot = null;
		while (reader.hasNext()) {
			reader.next();
			Integer id = Integer.parseInt(reader.get(EXPERIMENT_ID));

			if (!id.equals(currentRoot)) {
				closeExperiment(currentRoot, experiments);
				currentRoot = id;
			}

			int nodeId = Integer.parseInt(reader.get(NODE_ID));
			int sent = Integer.parseInt(reader.get(SENT));
			int received = Integer.parseInt(reader.get(RECEIVED));

			UnitExperiment experiment = experiments.get(id);
			if (experiment == null) {
				experiment = new UnitExperiment(id, fGraph.degree(id), cumulative, lastOnly);
				experiments.put(id, experiment);
			}

			experiment.addData(nodeId, sent, received);
		}

		// Wraps up the experiments.
		for (UnitExperiment experiment : experiments.values()) {
			experiment.done();
		}

		System.err.println("[OK]");
		return experiments;
	}

	private void closeExperiment(Integer root,
			Map<Integer, UnitExperiment> experiments) {
		if (root == null) {
			return;
		}

		UnitExperiment experiment = experiments.get(root);
		// Should never be null. If it is, then we want 
		// the exception to tell us there's a bug.
		experiment.done();
	}

}
