package it.unitn.disi.newscasting.experiments;

import peersim.core.Node;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;

/**
 * Simple metaheuristic which picks the proper heuristic based on a guarding
 * predicate.
 * 
 * @author giuliano
 */
public class PredicateHeuristic implements IPeerSelector {

	private final IPeerSelector fIf;

	private final IPeerSelector fElse;

	public PredicateHeuristic(IPeerSelector match, IPeerSelector dontMatch) {
		fIf = match;
		fElse = dontMatch;
	}

	@Override
	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	@Override
	public Node selectPeer(Node source, ISelectionFilter filter) {
		if(predicateMatches(source)) {
			return fIf.selectPeer(source, filter);
		} else {
			return fElse.selectPeer(source, filter);
		}
	}

	@Override
	public void clear(Node source) {
		fIf.clear(source);
		fElse.clear(source);
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}
	
	protected boolean predicateMatches(Node source) {
		return DisseminationExperimentGovernor.singletonInstance().currentNode() == source;
	}
}
