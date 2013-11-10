package it.unitn.disi.churn.config;

import it.unitn.disi.churn.config.AssignmentReader.Assignment;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.tabular.TableReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class ExperimentReader {

	/**
	 * File containing the experiment keys for us to simulate.
	 */
	@Attribute("sources")
	protected String fSources;

	/**
	 * File containing all neighborhood/node assignments.
	 */
	@Attribute(value = "assignments", defaultValue = Attribute.VALUE_NULL)
	protected String fAssignments;

	private String fIdField;

	/**
	 * Reader to the experiment stream.
	 */
	private TableReader fSourceReader;

	private FastRandomAssignmentReader fAssignmentReader;

	// -------------------------------------------------------------------------

	public ExperimentReader(@Attribute("id") String idField) {
		fIdField = idField;
	}

	// -------------------------------------------------------------------------

	public Experiment readExperiment(Integer row, IGraphProvider provider)
			throws Exception {

		if (shouldReadChurn()) {
			return readChurnByRow(row, provider);
		}

		return readStaticByRow(row);
	}

	// -------------------------------------------------------------------------

	private boolean shouldReadChurn() {
		return fAssignments != null;
	}

	// -------------------------------------------------------------------------

	private Experiment readChurnByRow(Integer row, IGraphProvider provider)
			throws Exception {
		seekSourceRow(row);
		int root = Integer.parseInt(fSourceReader.get(fIdField));

		return readChurn(provider, fSourceReader, root);
	}

	// -------------------------------------------------------------------------

	public Experiment readChurn(IGraphProvider provider,
			TableReader sourceReader, int root) throws IOException,
			RemoteException {
		Assignment a = readLIDI(root, provider.verticesOf(root));

		return new Experiment(root, sourceReader, a.li, a.di);
	}

	// -------------------------------------------------------------------------

	private Experiment readStaticByRow(Integer row) throws Exception {
		seekSourceRow(row);
		return staticExperiment(fSourceReader);
	}

	// -------------------------------------------------------------------------

	private Experiment staticExperiment(TableReader sourceReader) {
		int root = Integer.parseInt(sourceReader.get(fIdField));
		return new Experiment(root, sourceReader, null, null);
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

	private Assignment readLIDI(int root, int[] ids) throws IOException {
		Assignment a = new Assignment(ids.length);

		for (int i = 0; i < ids.length; i++) {
			fAssignmentReader.select(ids[i]);
			a.li[i] = fAssignmentReader.li();
			a.di[i] = fAssignmentReader.di();
			a.nodes[i] = i;
		}

		return a;
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

	public FastRandomAssignmentReader getAssignmentReader() {
		return fAssignmentReader;
	}

	// -------------------------------------------------------------------------

	public Iterator<Experiment> iterator(final IGraphProvider provider)
			throws IOException {

		final FileInputStream stream = new FileInputStream(new File(fSources));

		TableReader tmp = null;
		try {
			tmp = new TableReader(stream);
		} catch (IOException ex) {
			stream.close();
			throw ex;
		}

		final TableReader reader = tmp;

		return new Iterator<Experiment>() {

			@Override
			public boolean hasNext() {
				return reader.hasNext();
			}

			@Override
			public Experiment next() {
				try {
					reader.next();
					return readExperiment(
							Integer.parseInt(reader.get(fIdField)), provider);
				} catch (Exception ex) {
					throw MiscUtils.nestRuntimeException(ex);
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}
}
