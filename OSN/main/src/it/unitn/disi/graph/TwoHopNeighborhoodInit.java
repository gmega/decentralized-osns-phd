package it.unitn.disi.graph;

import peersim.config.AutoConfig;
import it.unitn.disi.utils.graph.LightweightStaticGraph;

@AutoConfig
public class TwoHopNeighborhoodInit extends GraphProtocolInit {
	
	@Override
	protected LightweightStaticGraph transform(LightweightStaticGraph graph) {
		return LightweightStaticGraph.transitiveGraph(super.transform(graph), 2);
	}
}
