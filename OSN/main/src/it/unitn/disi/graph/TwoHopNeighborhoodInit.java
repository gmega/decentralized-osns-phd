package it.unitn.disi.graph;

import it.unitn.disi.utils.graph.LightweightStaticGraph;

public class TwoHopNeighborhoodInit extends GraphProtocolInit {
	
	@Override
	protected LightweightStaticGraph transform(LightweightStaticGraph graph) {
		return LightweightStaticGraph.transitiveGraph(super.transform(graph), 2);
	}
}
