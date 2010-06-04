package it.unitn.disi.application;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.IPeerSamplingService;
import it.unitn.disi.protocol.selectors.ISelectionFilter;
import it.unitn.disi.utils.OrderingUtils;

import java.util.Random;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

public class FriendsInCommonSelector implements IPeerSamplingService, Protocol {

	private static final String PAR_PROTOCOL = "linkable";

	private static final String PAR_PSY = "psy";

	private static final String PAR_PSY_MINIMUM = "psymin";

	private int fLinkable;

	private int fSize = 0;

	private double fPsy;

	private int fPsyMinimum;

	private Random fRandom;

	private LinkableSortedFriendCollection fFriends;

	public FriendsInCommonSelector(String prefix) {
		this(Configuration.getPid(prefix + "." + PAR_PROTOCOL), Configuration
				.getDouble(prefix + "." + PAR_PSY), Configuration.getInt(prefix
				+ "." + PAR_PSY_MINIMUM), CommonState.r);
	}

	public FriendsInCommonSelector(int linkableId, double psy, int psyMinimum,
			Random random) {
		fLinkable = linkableId;
		fPsy = psy;
		fPsyMinimum = psyMinimum;
		fFriends = new LinkableSortedFriendCollection(fLinkable);
		fRandom = random;
	}

	public Node selectPeer(Node node) {
		return this.selectPeer(node, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	public boolean supportsFiltering() {
		return true;
	}

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
		if (fPsy <= 0) {
			end = fFriends.size() - 1;
		} else {
			end = (int) Math.max(0, fPsy * (double) fFriends.size());
			if (end < fPsyMinimum) {
				end = Math.min(fFriends.size(), fPsyMinimum);
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

	public FriendsInCommonSelector clone() {
		try {
			FriendsInCommonSelector adapter = (FriendsInCommonSelector) super
					.clone();
			return adapter;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}
}
