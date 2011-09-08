package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.IPushPeerSelector;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.MutableSimplePair;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.IReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * {@link BiasedCentralitySelector} picks more central nodes with higher or
 * lower probability.
 * 
 * @author giuliano
 */
@AutoConfig
public class BiasedCentralitySelector implements IPushPeerSelector, Protocol {

	private static boolean fPrintOnce = true;

	private IReference<Linkable> fLinkable;

	private IReference<IUtilityFunction<Node, Node>> fRanking;

	private final Random fRandom;

	private final boolean fAntiCentrality;

	public BiasedCentralitySelector(@Attribute("linkable") int linkable,
			@Attribute("ranking") int ranking,
			@Attribute("anticentrality") boolean anticentrality) {
		this(new ProtocolReference<Linkable>(linkable),
				new ProtocolReference<IUtilityFunction<Node, Node>>(ranking),
				anticentrality, CommonState.r);
	}

	public BiasedCentralitySelector(IReference<Linkable> linkableRef,
			IReference<IUtilityFunction<Node, Node>> ranking,
			boolean anticentrality, Random random) {
		fLinkable = linkableRef;
		fRanking = ranking;
		fRandom = random;
		fAntiCentrality = anticentrality;

		// Hack to get the centrality config printed without putting too much
		// crap on the logs.
		if (fPrintOnce) {
			System.out.println(BiasedCentralitySelector.class.getName()
					+ ": on "
					+ (fAntiCentrality ? "anticentrality" : "centrality")
					+ " mode.");
			fPrintOnce = false;
		}
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		return selectPeer(source, filter, null);
	}

	public Node selectPeer(Node source, ISelectionFilter filter,
			IGossipMessage update) {
		Linkable neighborhood = (Linkable) fLinkable.get(source);

		Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>> result = makeWheel(
				source, neighborhood, filter, update);
		// If we have a null, means the filter vetoed all neighbors.
		if (result == null) {
			return null;
		}

		RouletteWheel wheel = result.a;
		ArrayList<MutableSimplePair<Integer, Integer>> friends = result.b;
		return filter.selected(source,
				neighborhood.getNeighbor(friends.get(wheel.spin()).b));
	}

	private Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>> makeWheel(
			Node node, Linkable neighborhood, ISelectionFilter filter,
			IGossipMessage update) {
		ArrayList<MutableSimplePair<Integer, Integer>> fof = new ArrayList<MutableSimplePair<Integer, Integer>>();

		// XXX not good, need to generalize this concept of "anchor node" into
		// the IGossipMessage interface somehow.
		Node source = (update == null) ? node : ((Tweet) update).profile();

		IUtilityFunction<Node, Node> ranking = fRanking.get(source);
		// Computes the centrality metric.
		int total = 0;
		for (int i = 0; i < neighborhood.degree(); i++) {
			Node neighbor = neighborhood.getNeighbor(i);
			if (!filter.canSelect(source, neighbor)) {
				continue;
			}
			// Translates scores by 1 cause we cannot deal very well
			// with zeroes.
			int weight = ranking.utility(source, neighbor) + 1;
			total += weight;
			fof.add(new MutableSimplePair<Integer, Integer>(weight, i));
		}

		// If there are no neighbors to choose from, return null.
		if (fof.size() == 0) {
			return null;
		}

		// Sorts the array by the centrality metric (ascending scores).
		Collections.sort(fof,
				new Comparator<MutableSimplePair<Integer, Integer>>() {
					public int compare(MutableSimplePair<Integer, Integer> o1,
							MutableSimplePair<Integer, Integer> o2) {
						return o1.a - o2.a;
					}
				});

		// Now computes the probabilities.
		double[] probabs = new double[fof.size()];
		for (int i = 0; i < probabs.length; i++) {
			probabs[i] = ((double) fof.get(i).a) / total;
		}

		// Inverts the assignment if anticentrality.
		if (fAntiCentrality) {
			invertAssignment(probabs);
		}

		return new Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>>(
				new RouletteWheel(probabs, fRandom), fof);
	}

	@Override
	public void clear(Node source) {
		// We don't cache anything.
	}

	private void invertAssignment(double[] array) {
		int middle = array.length / 2;
		for (int i = 0; i < middle; i++) {
			int j = array.length - i - 1;
			double tmp = array[j];
			array[j] = array[i];
			array[i] = tmp;
		}
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			return super.clone();
		} catch (Exception ex) {
			throw new RuntimeException();
		}
	}
}
