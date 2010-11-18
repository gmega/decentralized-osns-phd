package it.unitn.disi.newscasting.internal.selectors;

import it.unitn.disi.graph.GraphUtils;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Centrality ranking based on node degree.
 * 
 * @author giuliano
 */
@AutoConfig
public class DegreeCentrality implements IUtilityFunction {

	private int fLinkable;

	/**
	 * If toggled, assigns higher score to lower centrality nodes.
	 */
	private boolean fAnticentrality;

	public DegreeCentrality(@Attribute("linkable") int linkable,
			@Attribute("anticentrality") boolean anticentrality) {
		fLinkable = linkable;
		fAnticentrality = anticentrality;
		System.err.println("Ranking function is "
				+ this.getClass().getSimpleName() + " operating in ["
				+ (fAnticentrality ? "anticentrality" : "centrality")
				+ "] mode.");
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public int utility(Node source, Node target) {
		int score = GraphUtils.countIntersections(source, target, fLinkable);
		if (fAnticentrality) {
			score = ((Linkable) source.getProtocol(fLinkable)).degree() - score;
		}
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
