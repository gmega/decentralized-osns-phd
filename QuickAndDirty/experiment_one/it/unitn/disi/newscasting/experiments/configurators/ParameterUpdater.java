package it.unitn.disi.newscasting.experiments.configurators;

import it.unitn.disi.unitsim.IUnitExperiment;
import it.unitn.disi.unitsim.cd.ICDExperimentObserver;
import it.unitn.disi.unitsim.cd.ICDUnitExperiment;
import it.unitn.disi.unitsim.experiments.NeighborhoodExperiment;
import it.unitn.disi.utils.tabular.IReference;
import it.unitn.disi.utils.tabular.TableReader;

import java.io.IOException;

import peersim.core.Linkable;
import peersim.core.Node;

public abstract class ParameterUpdater implements ICDExperimentObserver {

	private IReference<Linkable> fNeighborhood;

	private TableReader fReader;

	public ParameterUpdater(IReference<Linkable> neighborhood,
			TableReader reader) {
		fNeighborhood = neighborhood;
		fReader = reader;
	}

	@Override
	public void experimentStart(ICDUnitExperiment exp) {
		NeighborhoodExperiment nexp = (NeighborhoodExperiment) exp;
		Node root = nexp.rootNode();
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
	public void experimentEnd(ICDUnitExperiment exp) {
	}

	@Override
	public void experimentCycled(ICDUnitExperiment exp) {
	}

	protected abstract void update(Node node, TableReader reader);

	private void nextParameterSet() {
		try {
			fReader.next();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
