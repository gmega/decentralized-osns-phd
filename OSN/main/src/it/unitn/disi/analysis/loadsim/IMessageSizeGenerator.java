package it.unitn.disi.analysis.loadsim;

import peersim.graph.Graph;

/**
 * {@link IMessageSizeGenerator} generates message sizes according to an
 * underlying probability distribution, or another algorithm.
 * 
 * @author giuliano
 */
public interface IMessageSizeGenerator {
	/**
	 * @return size in bits for a message.
	 */
	public int nextSize(int nodeId, Graph graph);
}
