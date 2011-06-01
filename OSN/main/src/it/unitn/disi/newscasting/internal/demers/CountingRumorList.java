package it.unitn.disi.newscasting.internal.demers;

import java.util.Random;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.DenseMultiCounter;
import it.unitn.disi.utils.IKey;
import it.unitn.disi.utils.IMultiCounter;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.core.Linkable;
import peersim.core.Node;

public class CountingRumorList extends RumorList {

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

	public CountingRumorList(int maxSize, double giveupProbability,
			Linkable constraint, Random rnd) {

		super(maxSize, giveupProbability, constraint, rnd);

		Node[] neighbors = new Node[constraint.degree()];
		for (int i = 0; i < neighbors.length; i++) {
			neighbors[i] = constraint.getNeighbor(i);
		}
		fDestinations = new DenseMultiCounter<Node>(neighbors, fKeyer);
	}

	// ----------------------------------------------------------------------

	protected boolean addDestinations(IGossipMessage tweet) {
		int size = constraintLinkable().degree();
		int actual = 0;

		for (int i = 0; i < size; i++) {
			SNNode candidate = (SNNode) constraintLinkable().getNeighbor(i);
			if (!candidate.equals(tweet.originator())
					&& tweet.isDestination(candidate)) {
				fDestinations.increment(candidate);
				actual++;
			}
		}
		return actual != 0;
	}

	// ----------------------------------------------------------------------

	protected void removeDestinations(IGossipMessage tweet) {
		int size = constraintLinkable().degree();

		for (int i = 0; i < size; i++) {
			SNNode candidate = (SNNode) constraintLinkable().getNeighbor(i);
			if (!candidate.equals(tweet.originator())
					&& tweet.isDestination(candidate)) {
				fDestinations.decrement(candidate);
			}
		}
	}

	// ----------------------------------------------------------------------

	public int messagesFor(Node neighbor) {
		return fDestinations.count(neighbor);
	}

}
