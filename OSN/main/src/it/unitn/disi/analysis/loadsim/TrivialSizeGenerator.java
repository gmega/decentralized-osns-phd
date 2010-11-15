package it.unitn.disi.analysis.loadsim;

import peersim.graph.Graph;

/**
 * Returns size 1 messages. The output unit of the simulator is, then,
 * messages/round, and no longer bits/round.
 * 
 * @author giuliano
 */
public class TrivialSizeGenerator implements IMessageSizeGenerator {

	@Override
	public int nextSize(int nodeId, Graph graph) {
		return 1;
	}

}
