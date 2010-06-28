package it.unitn.disi.application.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;
import it.unitn.disi.application.interfaces.IPeerSelector;
import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.MutableSimplePair;
import it.unitn.disi.utils.Pair;
import it.unitn.disi.utils.peersim.ProtocolReference;

/**
 * Anti-centrality privileges nodes with low centrality. As it is, the
 * implementation is <b>very</b> inefficient, but at least I'm reasonable sure
 * that it does what it is supposed to.
 * 
 * @author giuliano
 */
public class AntiCentralitySelector implements IPeerSelector, Protocol {
	
	private static final String PAR_LINKABLE = "linkable";
	
	private IReference<Linkable> fLinkable;
	
	private final Random fRandom;
	
	public AntiCentralitySelector(String prefix) {
		this(new ProtocolReference<Linkable>(Configuration.getPid(prefix + "."
				+ PAR_LINKABLE)), CommonState.r);
	}
	
	public AntiCentralitySelector(IReference<Linkable> linkableRef, Random random) {
		fLinkable = linkableRef;
		fRandom = random;
	}

	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		Linkable neighborhood = (Linkable) fLinkable.get(source);
		
		Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>> result = makeWheel(
				neighborhood, filter);
		// If we have a null, means the filter vetoed all neighbors.
		if (result == null) {
			return null;
		}
		
		RouletteWheel wheel = result.a;
		ArrayList<MutableSimplePair<Integer, Integer>> friends = result.b;
		return filter.selected(neighborhood.getNeighbor(friends.get(wheel.spin()).b));
	}

	public boolean supportsFiltering() {
		return true;
	}
	
	private Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>> makeWheel(
			Linkable neighborhood, ISelectionFilter filter) {
		ArrayList<MutableSimplePair<Integer, Integer>> fof = new ArrayList<MutableSimplePair<Integer, Integer>>();

		// Computes the centrality metric.
		int total = 0;
		for (int i = 0; i < neighborhood.degree(); i++) {
			Node neighbor = neighborhood.getNeighbor(i);
			if(!filter.canSelect(neighbor)){
				continue;
			}
			int weight = centrality(neighborhood, neighbor);
			total += weight;
			fof.add(new MutableSimplePair<Integer, Integer>(weight, i));
		}
		
		// If there are no neighbors to choose from, return null.
		if (fof.size() == 0) {
			return null;
		}
		
		// Sorts the array by the centrality metric (lower centrality first).
		Collections.sort(fof,
				new Comparator<MutableSimplePair<Integer, Integer>>() {
					public int compare(MutableSimplePair<Integer, Integer> o1,
							MutableSimplePair<Integer, Integer> o2) {
						return o1.a - o2.a;
					}
				});
		
		// Inverts the centrality scores.
		int last = (int) Math.floor(fof.size()/2.0) - 1;
		for (int i = 0; i <= last ; i++) {
			Integer tmp = fof.get(i).a;
			fof.get(i).a = fof.get(fof.size() - i - 1).a;
			fof.get(fof.size() - i - 1).a = tmp;
		}
		
		// Now computes the probabilities.
		double [] probabs = new double[fof.size()];
		for (int i = 0; i < probabs.length; i++) {
			probabs[i] = ((double) fof.get(i).a)/total;
		}
				
		return new Pair<RouletteWheel, ArrayList<MutableSimplePair<Integer, Integer>>>(
				new RouletteWheel(probabs, fRandom), fof);
	}
	
	private int centrality(Linkable neighborhood, Node neighbor) {
		return MiscUtils.countIntersections(neighborhood, fLinkable
				.get(neighbor), null, false) + 1;
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
