package it.unitn.disi.churn.diffusion.churn;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.collections.Triplet;

public class CachingTransformer implements ILiveTransformer {

	private final ILiveTransformer fDelegate;

	private IndexedNeighborGraph fCachedSource;

	private Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> fCachedTransform;

	private double fVersion = -1.0;

	public CachingTransformer(ILiveTransformer delegate) {
		fDelegate = delegate;
	}

	@Override
	public Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> live(
			IndexedNeighborGraph source, INetwork network) {
		if (source != fCachedSource || fVersion != network.version()) {
			fCachedSource = source;
			fCachedTransform = fDelegate.live(source, network);
		}
		fVersion = network.version();
		return fCachedTransform;
	}

}
