package it.unitn.disi.protocol.selectors;

import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.protocol.IView;

import java.util.Random;

import peersim.core.Node;

/**
 * {@link RandomSelector} attempts to return a random node which satistifies the
 * filtering contraints. If one cannot be found, returns a random {@link Node}
 * from the underlying {@link IView}.
 * 
 * @author giuliano
 */
public class RandomSelector extends AbstractPeerSelector {

	private Random fRandom;

	public RandomSelector(IView view, Random random) {
		super(view);
		fRandom = random;
	}

	public Node selectPeer(ISelectionFilter filter) {
		// Shuffles the local view to make random selection simpler.
		fView.permute();

		for (int i = 0; i < fView.size(); i++) {
			Node peer = fView.getNode(i);
			if (filter.canSelect(peer)) {
				return filter.selected(peer);
			}
		}

		// No allowed peers in the view, return anything.
		return fView.getNode(fRandom.nextInt(fView.size()));
	}

}