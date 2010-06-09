package it.unitn.disi.application.selectors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.application.interfaces.IPeerSelector;
import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.MutableSimplePair;

/**
 * Anti-centrality privileges nodes with low centrality. As it is, the
 * implementation is <b>very</b> inefficient, but at least I'm reasonable sure
 * that it does what it is supposed to.
 * 
 * @author giuliano
 */
public class AntiCentralitySelector implements IPeerSelector {
	
	private static final String PAR_LINKABLE = "linkable";
	
	private int fLinkable;
	
	public AntiCentralitySelector(String prefix) {
		fLinkable = Configuration.getPid(prefix + "." + PAR_LINKABLE);
	}

	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public Node selectPeer(Node source, ISelectionFilter filter) {
		Linkable neighborhood = (Linkable) source.getProtocol(fLinkable);
		RouletteWheel wheel = makeWheel(neighborhood, filter);
		return neighborhood.getNeighbor(wheel.spin());
	}

	public boolean supportsFiltering() {
		return true;
	}
	
	private RouletteWheel makeWheel(Linkable neighborhood, ISelectionFilter filter) {
		ArrayList<MutableSimplePair<Integer, Integer>> fof = new ArrayList<MutableSimplePair<Integer, Integer>>();

		// Computes the centrality metric (friends-in-common, in this case).
		int total = 0;
		for (int i = 0; i < neighborhood.degree(); i++) {
			Node neighbor = neighborhood.getNeighbor(i);
			if(!filter.canSelect(neighbor)){
				continue;
			}
			int weight = MiscUtils.countIntersections(neighborhood, neighbor,
					fLinkable) + 1;
			total += weight;
			fof.add(new MutableSimplePair<Integer, Integer>(weight, i));
		}
		
		// Sorts the array by the centrality metric (lower centrality first).
		Collections.sort(fof,
				new Comparator<MutableSimplePair<Integer, Integer>>() {
					@Override
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
			probabs[fof.get(i).b] = ((double) fof.get(i).a)/total;
		}
		
		return new RouletteWheel(probabs, CommonState.r);
	}

}
