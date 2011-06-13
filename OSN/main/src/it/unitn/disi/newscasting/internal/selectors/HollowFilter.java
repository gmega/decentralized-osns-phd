package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.epidemics.ISelectionFilter;
import peersim.core.Node;

public class HollowFilter implements ISelectionFilter {

	private ISelectionFilter fDelegate;

	@Override
	public Node selected(Node source, Node node) {
		return fDelegate.selected(source, node);
	}

	@Override
	public boolean canSelect(Node source, Node node) {
		return fDelegate.canSelect(source, node);
	}

	public void bind(ISelectionFilter delegate) {
		fDelegate = delegate;
	}
}