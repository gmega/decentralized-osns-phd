package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.Random;

import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Rumor mongering list with several optimizations which make the protocol
 * orders of magnitude faster when running under the unit experiment framework.
 * 
 * @author giuliano
 */
public class UEOptimizedRumorList extends RumorList {

	private IGossipMessage fCurrent;

	public UEOptimizedRumorList(int maxSize, double giveupProbability,
			Linkable constraint, Random rnd) {
		super(maxSize, giveupProbability, constraint, rnd);
	}

	@Override
	protected boolean addDestinations(IGossipMessage message) {
		if (fCurrent != null) {
			throw new IllegalStateException();
		}
		/*
		 * We know we get only one "dissemination session" at a time, so all we
		 * have to do is check whether we have ONE destination that's possibly
		 * valid.
		 */
		Linkable lnk = constraintLinkable();
		int degree = lnk.degree();
		for (int i = 0; i < degree; i++) {
			SNNode neighbor = (SNNode) lnk.getNeighbor(i);
			if (neighbor.isActive()) {
				fCurrent = message;
				return true;
			}
		}
		return false;
	}

	@Override
	protected void removeDestinations(IGossipMessage message) {
		fCurrent = null;
	}

	@Override
	protected int messagesFor(Node node) {
		if (fCurrent == null) {
			return 0;
		}
		/*
		 * Again, we know we only have one session at a time, so if the node is
		 * active and and we have a message, then it's for that node.
		 */
		SNNode snode = (SNNode) node;
		if (snode.isActive() && size > 0 && !node.equals(fCurrent.originator())) {
			return 1;
		}
		return 0;
	}
}
