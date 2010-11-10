package it.unitn.disi.newscasting.experiments.configurators;

import java.io.IOException;

import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.newscasting.experiments.IExperimentObserver;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.TableReader;

public abstract class ParameterUpdater implements IExperimentObserver {

	private IReference<Linkable> fNeighborhood;

	private TableReader fReader;

	private boolean fFirst = true;

	public ParameterUpdater(IReference<Linkable> neighborhood,
			TableReader reader) {
		fNeighborhood = neighborhood;
		fReader = reader;
	}

	@Override
	public void experimentStart(Node root) {
		nextParameterSet();		
		long id = Long.parseLong(fReader.get("id"));

		// Check that the file is properly ordered.
		if (id != root.getID()) {
			throw new IllegalStateException(
					"Parameter file is not properly ordered. "
							+ "It must be ordered as in the schedule (" + id
							+ " != " + root.getID() + ")");
		}

		update(root, fReader);
		Linkable neighborhood = fNeighborhood.get(root);
		int degree = neighborhood.degree();

		for (int i = 0; i < degree; i++) {
			Node neighbor = neighborhood.getNeighbor(i);
			update(neighbor, fReader);
		}
	}

	@Override
	public void experimentEnd(Node root) {
	}

	@Override
	public void experimentCycled(Node root) {
	}

	protected abstract void update(Node node, TableReader reader);

	private void nextParameterSet() {
		// If it's the first time the method is called,
		// no need to call next as the table reader already
		// has a set of parameters to return.
		if (fFirst) {
			fFirst = false;
			return;
		}

		// Next parameter.
		try {
			fReader.next();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
