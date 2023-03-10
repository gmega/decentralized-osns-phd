package it.unitn.disi.sps.selectors;

import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.sps.newscast.IView;
import peersim.core.Node;

public class HighestAgeSelector extends AbstractPeerSelector {

	public HighestAgeSelector(IView view) {
		super(view);
	}

	public Node selectPeer(ISelectionFilter filter) {
		int max = -1;
		int max_idx = -1;
		int size = fView.size();

		// Selects the oldest. It's not sorted so we
		// have to go one by one.
		for (int i = size - 1; i >= 0; i--) {
			int candidate = fView.getTimestamp(i);
			if (candidate > max) {
				max = candidate;
				max_idx = i;
			}
		}

		Node node = fView.getNode(max_idx);
		if (filter.canSelect(null, node)) {
			return filter.selected(null, node);
		}

		return filter.selected(null, null);
	}
}

