package it.unitn.disi.churn.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.MissingParameterException;

import it.unitn.disi.churn.config.AssignmentReader.Assignment;
import it.unitn.disi.churn.config.IndexedReader.IndexEntry;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.tabular.TableReader;

@AutoConfig
public class ExperimentReader {

	/**
	 * File containing the experiment keys for us to simulate.
	 */
	@Attribute("sources")
	protected String fSources;

	/**
	 * Whether or not we should read churn assignments for these experiments.
	 */
	@Attribute("churn")
	protected boolean fChurn;

	/**
	 * File containing all neighborhood/node assignments.
	 */
	@Attribute(value = "assignments", defaultValue = "none")
	protected String fAssignments;

	/**
	 * Index with the entry position of each neighborhood in the neighborhood/
	 * node assignments file.
	 */
	@Attribute(value = "index", defaultValue = "none")
	protected String fAssignmentIndex;

	private String fIdField;

	/**
	 * Reader to the experiment stream.
	 */
	private TableReader fSourceReader;

	private IndexedReader fAssigIndex;

	private AssignmentReader fAssigReader;

	// -------------------------------------------------------------------------

	public ExperimentReader(String idField) {
		fIdField = idField;
	}

	// -------------------------------------------------------------------------

	public Experiment readExperiment(Integer row, IGraphProvider provider)
			throws Exception {
		if (fChurn) {
			checkConfig();
			return readChurn(row, provider);
		}

		return readStatic(row);
	}

	// -------------------------------------------------------------------------

	private void checkConfig() {
		if (fChurn
				&& (fAssignments.equals("none") || fAssignmentIndex
						.equals("none"))) {
			throw new MissingParameterException("assignments or index");
		}
	}

	// -------------------------------------------------------------------------

	private Experiment readChurn(Integer row, IGraphProvider provider)
			throws Exception {
		seekSourceRow(row);
		int root = Integer.parseInt(fSourceReader.get(fIdField));

		Pair<Assignment, IndexEntry> assignment = readLIDI(root,
				provider.verticesOf(root));

		return new Experiment(root, fSourceReader, assignment.a.li,
				assignment.a.di, assignment.b);
	}

	// -------------------------------------------------------------------------

	private Experiment readStatic(Integer row) throws Exception {
		seekSourceRow(row);
		int root = Integer.parseInt(fSourceReader.get(fIdField));

		return new Experiment(root, fSourceReader, null, null, null);
	}

	// -------------------------------------------------------------------------

	public TableReader sourceReader() throws IOException {
		if (fSourceReader == null) {
			resetReader();
		}
		return fSourceReader;
	}

	// -------------------------------------------------------------------------

	private void resetReader() throws IOException {
		File f = new File(fSources);
		System.err
				.println("-- Source will be taken from [" + f.getName() + "]");
		fSourceReader = new TableReader(new FileInputStream(f));
	}

	// -------------------------------------------------------------------------

	private Pair<Assignment, IndexEntry> readLIDI(int root, int[] ids)
			throws IOException {
		IndexEntry entry;

		if (fAssigIndex == null) {
			fAssigIndex = IndexedReader.createReader(
					new File(fAssignmentIndex), new File(fAssignments));
			fAssigReader = new AssignmentReader(fAssigIndex.getStream(), "id");
		}

		if ((entry = fAssigIndex.select(root)) == null) {
			throw new NoSuchElementException();
		}

		fAssigReader.streamRepositioned();
		Assignment ass = (Assignment) fAssigReader.read(ids);

		return new Pair<Assignment, IndexEntry>(ass, entry);
	}

	// -------------------------------------------------------------------------

	public void seekSourceRow(Integer row) throws IOException {
		if (row < sourceReader().currentRow()) {
			resetReader();
		}

		while (row > sourceReader().currentRow()) {
			sourceReader().next();
		}
	}

	// -------------------------------------------------------------------------

	public static class Experiment {

		public final int root;

		public final Map<String, String> attributes;

		public final double[] lis;
		public final double[] dis;

		public final IndexEntry entry;

		public Experiment(int root, TableReader source, double[] lis,
				double[] dis, IndexEntry entry) {
			this.root = root;

			HashMap<String, String> attributes = new HashMap<String, String>();
			for (String key : source.columns()) {
				attributes.put(key, source.get(key));
			}
			this.attributes = Collections.unmodifiableMap(attributes);

			this.lis = lis;
			this.dis = dis;
			this.entry = entry;
		}

		public String toString() {
			return "root: " + root;
		}
	}

}
