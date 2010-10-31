package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.graph.GraphUtils;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;

@AutoConfig
public class DegreeCentrality implements IUtilityFunction {

	private int fLinkable;
	
	public DegreeCentrality(
			@Attribute("linkable") int linkable) {
		fLinkable = linkable;
	}
	
	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public int utility(Node source, Node target){
		return GraphUtils.countIntersections(source, target, fLinkable);
	}

	public Object clone() {
		return this;
	}
}
