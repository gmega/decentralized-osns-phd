package it.unitn.disi;


import org.easymock.EasyMock;

import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.Protocol;

public class TestLinkable {

	public static TestLinkable testLinkable(int[][] ids, int protocolID) {
		TestLinkable lnk = new TestLinkable(ids);
		for (Node node : lnk.fNodes) {
			TestUtils.addProtocol(node, protocolID, lnk.linkableOf(lnk.indexOf(node)));
		}
		return lnk;
	}

	private Node[] fNodes;

	private int[][] fNeighArray;

	private TestLinkable(int[][] ids) {
		fNodes = new Node[ids.length];
		for (int i = 0; i < ids.length; i++) {
			fNodes[i] = TestUtils.baseNode();
		}
		fNeighArray = ids;
	}
	
	public void replayAll() {
		EasyMock.replay((Object [])fNodes);
	}

	public Node get(int i) {
		return fNodes[i];
	}

	private Protocol linkableOf(int id) {
		return new LinkableView(id);
	}

	public int indexOf(Node node) {
		for (int i = 0; i < fNodes.length; i++) {
			if (fNodes[i] == node) {
				return i;
			}
		}

		return -1;
	}

	class LinkableView implements Linkable, Protocol {
		private int fId;

		public LinkableView(int id) {
			fId = id;
		}

		public boolean contains(Node neighbor) {
			int idx = indexOf(neighbor);
			for (int i = 0; i < fNeighArray[fId].length; i++) {
				if (fNeighArray[fId][i] == idx) {
					return true;
				}
			}

			return false;
		}

		public int degree() {
			return fNeighArray[fId].length;
		}

		public Node getNeighbor(int i) {
			return fNodes[fNeighArray[fId][i]];
		}

		public Object clone() {
			return null;
		}

		public void pack() {
		}

		public void onKill() {
		}

		public boolean addNeighbor(Node neighbour) {
			throw new UnsupportedOperationException();
		}
	}
}
