package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MutableSimplePair;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.peersim.ProtocolReference;

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
public class BiasedCentralitySelector implements IPeerSelector, Protocol {

	private IReference<Linkable> fLinkable;

	private IReference<IUtilityFunction> fRanking;

	private final Random fRandom;

	private final boolean fAntiCentrality;

	public BiasedCentralitySelector(@Attribute("linkable") int linkable,
			@Attribute("ranking") int ranking,
			@Attribute("anticentrality") boolean anticentrality) {
		this(new ProtocolReference<Linkable>(linkable),
				new ProtocolReference<IUtilityFunction>(ranking),
				anticentrality, CommonState.r);
	}

	public BiasedCentralitySelector(IReference<Linkable> linkableRef,
			IReference<IUtilityFunction> ranking, boolean anticentrality,
			Random random) {
		fLinkable = linkableRef;
		fRanking = ranking;
		fRandom = random;
		fAntiCentrality = anticentrality;
	}

	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		Linkable neighborhood = (Linkable) fLinkable.get(source);

		Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>> result = makeWheel(
				source, neighborhood, filter);
		// If we have a null, means the filter vetoed all neighbors.
		if (result == null) {
			return null;
		}

		RouletteWheel wheel = result.a;
		ArrayList<MutableSimplePair<Integer, Integer>> friends = result.b;
		return filter.selected(neighborhood.getNeighbor(friends.get(wheel
				.spin()).b));
	}

	public boolean supportsFiltering() {
		return true;
	}

	private Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>> makeWheel(
			Node source, Linkable neighborhood, ISelectionFilter filter) {
		ArrayList<MutableSimplePair<Integer, Integer>> fof = new ArrayList<MutableSimplePair<Integer, Integer>>();
		IUtilityFunction ranking = fRanking.get(source);
		// Computes the centrality metric.
		int total = 0;
		for (int i = 0; i < neighborhood.degree(); i++) {
			Node neighbor = neighborhood.getNeighbor(i);
			if (!filter.canSelect(neighbor)) {
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
	
	private void invertAssignment(double [] array) {
		int middle = array.length/2;
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
