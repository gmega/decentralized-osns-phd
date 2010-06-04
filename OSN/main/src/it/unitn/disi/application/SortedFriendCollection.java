package it.unitn.disi.application;

import it.unitn.disi.utils.IExchanger;
import it.unitn.disi.utils.MiscUtils;

import java.util.Arrays;

import peersim.core.Linkable;
import peersim.core.Node;

public abstract class SortedFriendCollection implements IExchanger {

	private FriendDescriptor fFriends[];

	private int fSize;

	protected int fLinkableId;

	SortedFriendCollection(int linkableId) {
		fLinkableId = linkableId;
	}

	public int size() {
		return fSize;
	}

	public Node get(int idx) {
		return fFriends[idx].fNode;
	}
	
	public int friendsInCommon(int idx) {
		return fFriends[idx].fInCommon;
	}
	
	public void exchange(int i, int j) {
		FriendDescriptor tmp = fFriends[i];
		fFriends[i] = fFriends[j];
		fFriends[j] = tmp;
	}

	protected abstract Node neighbor(int i, Object collection);

	protected void sortByFriendsInCommon(Linkable ours, Object collection, int size) {
		resizeCaches(size);

		for (int i = 0; i < size; i++) {
			Node node = neighbor(i, collection);
			fFriends[i].fInCommon = MiscUtils.countIntersections(ours, node, fLinkableId);
			fFriends[i].fNode = node;
		}

		Arrays.sort(fFriends, 0, size);
	}

	private void resizeCaches(int size) {
		if (fSize < size) {
			fFriends = new FriendDescriptor[size];
			for (int i = 0; i < size; i++) {
				fFriends[i] = new FriendDescriptor();
			}
		}

		fSize = size;
	}

	private class FriendDescriptor implements Comparable<FriendDescriptor> {

		public int fInCommon;
		public Node fNode;

		public int compareTo(FriendDescriptor o) {
			return o.fInCommon - fInCommon;
		}

	}
}

