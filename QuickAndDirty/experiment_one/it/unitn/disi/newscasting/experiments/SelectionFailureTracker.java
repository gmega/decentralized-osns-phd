package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.utils.tabular.IReference;
import peersim.core.Node;

/**
 * {@link ISelectionFilter} wrapper which intercepts selection failures.
 * 
 * @author giuliano
 */
public class SelectionFailureTracker implements ISelectionFilter {

	private IReference<ISelectionFilter> fDelegate;

	public SelectionFailureTracker(IReference<ISelectionFilter> delegate) {
		fDelegate = delegate;
	}

	@Override
	public boolean canSelect(Node source, Node candidate) {
		ISelectionFilter filter = fDelegate.get(source);
		return filter.canSelect(source, candidate);
	}

	@Override
	public Node selected(Node source, Node candidate) {
		if (candidate == null) {
			ExperimentStatisticsManager mgr = ExperimentStatisticsManager
					.getInstance();
			mgr.noSelection(source);
		}
		ISelectionFilter filter = fDelegate.get(source);
		return filter.selected(source, candidate);
	}

}
