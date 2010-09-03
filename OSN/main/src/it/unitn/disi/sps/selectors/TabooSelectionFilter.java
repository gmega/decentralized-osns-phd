package it.unitn.disi.sps.selectors;

import it.unitn.disi.ISelectionFilter;
import peersim.config.Configuration;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * 
 * @author giuliano
 */
public class TabooSelectionFilter implements ISelectionFilter, Protocol {
	
	private static final String PAR_R = "r";

	private Node[] fTabooList;

	private int fCounter = 0;
	
	public TabooSelectionFilter(String s) {
		this(Configuration.getInt(s + "." + PAR_R));
	}

	public TabooSelectionFilter(int r) {
		fTabooList = new Node[r];
	}

	public Node selected(Node peer) {
		fTabooList[fCounter] = peer;
		fCounter = (fCounter + 1) % fTabooList.length;
		return peer;
	}

	public boolean canSelect(Node peer) {
		
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

