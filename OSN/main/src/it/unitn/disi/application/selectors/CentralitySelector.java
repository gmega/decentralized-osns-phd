package it.unitn.disi.application.selectors;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.application.LinkableSortedFriendCollection;
import it.unitn.disi.application.interfaces.IPeerSelector;
import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.utils.OrderingUtils;

import java.util.Random;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * {@link CentralitySelector} biases selection towards nodes with higher
 * centrality.
 * 
 * @author giuliano
 */
public class CentralitySelector implements IPeerSelector, Protocol {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	
	private static final String PAR_PROTOCOL = "linkable";

	private static final String PAR_PSI = "psi";

	private static final String PAR_PSI_MINIMUM = "psimin";
	
	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private int fLinkable;

	private double fPsi;

	private int fPsiMinimum;
	
	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	private int fSize = 0;

	private Random fRandom;

	private LinkableSortedFriendCollection fFriends;
	
	// ----------------------------------------------------------------------

	public CentralitySelector(String prefix) {
		this(Configuration.getPid(prefix + "." + PAR_PROTOCOL), Configuration
				.getDouble(prefix + "." + PAR_PSI), Configuration.getInt(prefix
				+ "." + PAR_PSI_MINIMUM), CommonState.r);
	}
	
	// ----------------------------------------------------------------------

	public CentralitySelector(int linkableId, double psy, int psyMinimum,
			Random random) {
		fLinkable = linkableId;
		fPsi = psy;
		fPsiMinimum = psyMinimum;
		fFriends = new LinkableSortedFriendCollection(fLinkable);
		fRandom = random;
	}
	
	// ----------------------------------------------------------------------
	// IPeerSelector interface.
	// ----------------------------------------------------------------------

	public Node selectPeer(Node node) {
		return this.selectPeer(node, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}
	
	// ----------------------------------------------------------------------

	public boolean supportsFiltering() {
		return true;
	}
	
	// ----------------------------------------------------------------------

	public Node selectPeer(Node source, ISelectionFilter filter) {
		// Step 1 - sorts list by friends in common (if the underlying
		// view has changed.
		Linkable linkable = (Linkable) source.getProtocol(fLinkable);
		if (hasChanged(linkable)) {
			fFriends.sortByFriendsInCommon(source);
		}

		// Step 2 - selects a head size which is either composed by
		// the fPsy percent largest elements, or by fPsyMinimum,
		// if fPsy percent is smaller than fPsyMinimum.
		int end;
		if (fPsi <= 0) {
			end = fFriends.size() - 1;
		} else {
			end = (int) Math.max(0, fPsi * (double) fFriends.size());
			if (end < fPsiMinimum) {
				end = Math.min(fFriends.size(), fPsiMinimum);
			}
		}

		// Step 3 - scrambles the head.
		OrderingUtils.permute(0, end, fFriends, fRandom);

		// Step 4 - tries to pick someone up.
		for (int i = 0; i < end; i++) {
			if (filter.canSelect(fFriends.get(i))) {
				return filter.selected(fFriends.get(i));
			}
		}

		return filter.selected(null);
	}
	
	// ----------------------------------------------------------------------

	private boolean hasChanged(Linkable linkable) {
		boolean changed = true;
		if (linkable instanceof IDynamicLinkable) {
			changed = ((IDynamicLinkable) linkable).hasChanged(CommonState
					.getIntTime());
		}

		// Needless to say, IDynamicLinkables should be used
		// for anything but the smallest simulations.
		return changed || fSize == 0;
	}
	
	// ----------------------------------------------------------------------
	// Protocol interface.
	// ----------------------------------------------------------------------

	public CentralitySelector clone() {
		try {
			CentralitySelector adapter = (CentralitySelector) super
					.clone();
			return adapter;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
	
	// ----------------------------------------------------------------------
}
