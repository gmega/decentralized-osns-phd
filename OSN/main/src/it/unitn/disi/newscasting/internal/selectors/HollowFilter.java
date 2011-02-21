package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.ISelectionFilter;
import peersim.core.Node;

public class HollowFilter implements ISelectionFilter {

	private ISelectionFilter fDelegate;

	@Override
	public Node selected(Node node) {
		return fDelegate.selected(node);
	}

	@Override
	public boolean canSelect(Node node) {
		return fDelegate.canSelect(node);
	}

	public void bind(ISelectionFilter delegate) {
		fDelegate = delegate;
	}
}