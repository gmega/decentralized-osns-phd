package it.unitn.disi.newscasting;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.utils.IReference;
import peersim.core.Node;

public class BinaryCompositeFilter implements ISelectionFilter {

	private final IReference<ISelectionFilter> fLeft;

	private final IReference<ISelectionFilter> fRight;

	public BinaryCompositeFilter(IReference<ISelectionFilter> left,
			IReference<ISelectionFilter> right) {
		fLeft = left;
		fRight = right;
	}

	@Override
	public boolean canSelect(Node source, Node candidate) {
		return fLeft.get(source).canSelect(source, candidate)
				&& fRight.get(source).canSelect(source, candidate);
	}

	@Override
	public Node selected(Node source, Node peer) {
		fLeft.get(source).selected(source, peer);
		fRight.get(source).selected(source, peer);
		return peer;
	}

	public IReference<ISelectionFilter> left() {
		return fLeft;
	}

	public IReference<ISelectionFilter> right() {
		return fRight;
	}
}
