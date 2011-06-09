package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.DenseMultiCounter;
import it.unitn.disi.utils.IKey;
import it.unitn.disi.utils.IMultiCounter;
import it.unitn.disi.utils.peersim.SNNode;

import peersim.core.Linkable;
import peersim.core.Node;

public class CountingDestinationTracker implements IDestinationTracker {

	private static IKey<Node> fKeyer = new IKey<Node>() {
		@Override
		public int key(Node element) {
			return (int) element.getID();
		}
	};

	/**
	 * Keeps track of destinations for messages.
	 */
	private IMultiCounter<Node> fDestinations;
	
	private Linkable fConstraint;

	public CountingDestinationTracker(Linkable constraint) {
		Node[] neighbors = new Node[constraint.degree()];
		for (int i = 0; i < neighbors.length; i++) {
			neighbors[i] = constraint.getNeighbor(i);
		}
		fDestinations = new DenseMultiCounter<Node>(neighbors, fKeyer);
		fConstraint = constraint;
	}

	// ----------------------------------------------------------------------

	public Result track(IGossipMessage tweet) {
		int size = fConstraint.degree();
		int actual = 0;
		boolean originator = false;

		for (int i = 0; i < size; i++) {
			SNNode candidate = (SNNode) fConstraint.getNeighbor(i);
			if (tweet.isDestination(candidate)) {
				if (candidate.equals(tweet.originator())) {
					originator = true;
				} else {
					fDestinations.increment(candidate);
					actual++;
				}
			}
		}

		if (actual == 0) {
			return originator ? Result.originator_only : Result.no_intersection;
		} else {
			return Result.forward;
		}
	}

	// ----------------------------------------------------------------------

	public void drop(IGossipMessage tweet) {
		int size = fConstraint.degree();

		for (int i = 0; i < size; i++) {
			SNNode candidate = (SNNode) fConstraint.getNeighbor(i);
			if (!candidate.equals(tweet.originator())
					&& tweet.isDestination(candidate)) {
				fDestinations.decrement(candidate);
			}
		}
	}

	// ----------------------------------------------------------------------

	public int count(Node neighbor) {
		return fDestinations.count(neighbor);
	}

	@Override
	public Linkable constraint() {
		return fConstraint;
	}

}
