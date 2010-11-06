package it.unitn.disi.newscasting.experiments.configurators;

import java.io.IOException;

import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.newscasting.experiments.IExperimentObserver;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.TableReader;

public abstract class ParameterUpdater<K> implements IExperimentObserver {

	private IReference <Linkable> fNeighborhood;
	
	private TableReader fReader;

	public ParameterUpdater(IReference<Linkable> neighborhood) {
		fNeighborhood = neighborhood;
	}

	@Override
	public void experimentStart(Node root) {
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
		try {
			fReader.next();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}	
	}

	@Override
	public void experimentCycled(Node root) { }

	protected abstract void update (Node node, TableReader reader);
}
