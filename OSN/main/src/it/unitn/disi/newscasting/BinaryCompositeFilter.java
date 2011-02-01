package it.unitn.disi.newscasting;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.utils.IReference;
import peersim.core.Node;

public class BinaryCompositeFilter implements ISelectionFilter {
	
	private final IReference<ISelectionFilter> fLeft;
	
	private final IReference<ISelectionFilter> fRight;
	
	private Node fOwner;
	
	public BinaryCompositeFilter(IReference<ISelectionFilter> left, IReference<ISelectionFilter> right) {
		fLeft = left;
		fRight = right;
	}
	
	public void bind(Node node) {
		if (fOwner != null) {
			throw new IllegalStateException("Binding hasn't been properly cleared.");
		}
		fOwner = node;
	}
	
	public void clear() {
		fOwner = null;
	}

	@Override
	public boolean canSelect(Node node) {
		return fLeft.get(fOwner).canSelect(node) && fRight.get(fOwner).canSelect(node);
	}

	@Override
	public Node selected(Node node) {
		fLeft.get(fOwner).selected(node);
		fRight.get(fOwner).selected(node);
		return node;
	}
	
	public IReference<ISelectionFilter> left() {
		return fLeft;
	}
	
	public IReference<ISelectionFilter> right() {
		return fRight;
	}
}
