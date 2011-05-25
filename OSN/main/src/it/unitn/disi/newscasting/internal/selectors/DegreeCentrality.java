package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.graph.GraphUtils;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * Centrality ranking based on node degree.
 * 
 * @author giuliano
 */
@AutoConfig
public class DegreeCentrality implements IUtilityFunction<Node, Node>, Protocol {

	private int fLinkable;

	public DegreeCentrality(@Attribute("linkable") int linkable) {
		fLinkable = linkable;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public int utility(Node source, Node target) {
		int score = GraphUtils.countIntersections(source, target, fLinkable);
		// Sanity test
		if (score < 0) {
			throw new InternalError(Integer.toString(score));
		}
		return score;
	}

	public Object clone() {
		return this;
	}
}
