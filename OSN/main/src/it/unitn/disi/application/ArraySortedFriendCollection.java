package it.unitn.disi.application;

import peersim.core.Linkable;
import peersim.core.Node;


public class ArraySortedFriendCollection extends SortedFriendCollection {

	ArraySortedFriendCollection(int linkableId) {
		super(linkableId);
		fLinkableId = linkableId;
	}

	public void recomputeRankings(Node source, Node[] nodes, int length) {
		Linkable linkable = (Linkable) source.getProtocol(fLinkableId);
		super.sortByFriendsInCommon(linkable, nodes, length);
	}

	protected Node neighbor(int i, Object collection) {
		return ((Node [])collection)[i];
	}

}

