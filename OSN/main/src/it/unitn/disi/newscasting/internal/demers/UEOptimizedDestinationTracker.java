package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.peersim.SNNode;

import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Rumor mongering list with several optimizations which make the protocol
 * orders of magnitude faster when running under the unit experiment framework.
 * 
 * @author giuliano
 */
public class UEOptimizedDestinationTracker implements IDestinationTracker {

	private final Linkable fConstraint;
	
	private IGossipMessage fCurrent;

	public UEOptimizedDestinationTracker(Linkable constraint) {
		fConstraint = constraint;
	}

	@Override
	public Result track(IGossipMessage message) {
		if (fCurrent != null) {
			throw new IllegalStateException();
		}
		/*
		 * We know we get only one "dissemination session" at a time, so all we
		 * have to do is check whether we have ONE destination that's active.
		 */
		int degree = fConstraint.degree();
		for (int i = 0; i < degree; i++) {
			SNNode neighbor = (SNNode) fConstraint.getNeighbor(i);
			if (neighbor.isActive()) {
				fCurrent = message;
				return Result.forward;
			}
		}
		return Result.no_intersection;
	}

	@Override
	public void drop(IGossipMessage message) {
		fCurrent = null;
	}

	@Override
	public int count(Node node) {
		if (fCurrent == null) {
			return 0;
		}
		/*
		 * Again, we know we only have one session at a time, so if the node is
		 * active and and we have a message, then it's for that node.
		 */
		SNNode snode = (SNNode) node;
		if (snode.isActive() && !node.equals(fCurrent.originator())) {
			return 1;
		}
		return 0;
	}
	
	@Override
	public Linkable constraint() {
		return fConstraint;
	}
}
