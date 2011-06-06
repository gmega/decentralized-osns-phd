package it.unitn.disi.sps.selectors;

import it.unitn.disi.ISelectionFilter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * A simple {@link ISelectionFilter} implementation providing a taboo list.
 * 
 * @author giuliano
 */
@AutoConfig
public class TabooSelectionFilter implements ISelectionFilter, Protocol {

	private Node[] fTabooList;

	private int fCounter = 0;

	public TabooSelectionFilter(@Attribute("listsize") int r) {
		fTabooList = new Node[r];
	}

	public Node selected(Node source, Node peer) {
		fTabooList[fCounter] = peer;
		fCounter = (fCounter + 1) % fTabooList.length;
		return peer;
	}

	public boolean canSelect(Node source, Node peer) {

		if (!peer.isUp()) {
			return false;
		}

		for (int i = 0; i < fTabooList.length; i++) {
			if (fTabooList[i] == peer) {
				return false;
			}
		}
		return true;
	}

	public Object clone() {
		TabooSelectionFilter clone;
		try {
			clone = (TabooSelectionFilter) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}

		clone.fCounter = fCounter;
		clone.fTabooList = new Node[clone.fTabooList.length];
		System.arraycopy(fTabooList, 0, clone.fTabooList, 0, fTabooList.length);

		return clone;
	}

}
