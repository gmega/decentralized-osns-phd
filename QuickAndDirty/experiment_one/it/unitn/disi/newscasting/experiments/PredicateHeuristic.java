package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.unitsim.CDGovernor;
import it.unitn.disi.unitsim.experiments.NeighborhoodExperiment;
import peersim.core.Node;

/**
 * Simple metaheuristic which picks the proper heuristic based on a guarding
 * predicate.
 * 
 * @author giuliano
 */
public class PredicateHeuristic implements IPeerSelector {

	private final IPeerSelector fIf;

	private final IPeerSelector fElse;
	
	private final CDGovernor fGovernor;

	public PredicateHeuristic(IPeerSelector match, IPeerSelector dontMatch,
			CDGovernor governor) {
		fIf = match;
		fElse = dontMatch;
		fGovernor = governor;
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
	
	protected boolean predicateMatches(Node source) {
		return ((NeighborhoodExperiment) fGovernor.currentExperiment())
				.rootNode() == source;
	}
}
