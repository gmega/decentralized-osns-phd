package it.unitn.disi.application;

import peersim.core.Linkable;
import peersim.core.Node;

public class LinkableSortedFriendCollection extends SortedFriendCollection {

	public LinkableSortedFriendCollection(int linkableId) {
		super(linkableId);
		fLinkableId = linkableId;
	}

	public void sortByFriendsInCommon(Node source) {
		Linkable linkable = (Linkable) source.getProtocol(fLinkableId);
		super.sortByFriendsInCommon(linkable, linkable, linkable.degree());
	}

	protected Node neighbor(int i, Object collection) {
		return ((Linkable)collection).getNeighbor(i);
	}
}
