package it.unitn.disi.unitsim;

import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;
import it.unitn.disi.graph.CompleteGraph;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;

/**
 * Very limited but memory-efficient implementation of
 * {@link IGraphProvider} for generating complete neighborhoods. <BR>
 * <BR>
 * This implementation does not respect the {@link IGraphProvider}
 * contract for method {@link #verticesOf(Integer)}, and the values returned by
 * this method do not represent a cohesive underlying graph.
 * 
 * 
 * @author giuliano
 */
@AutoConfig
public class CompleteNeighborhoodGenerator implements IGraphProvider,
		IPlugin {

	public CompleteNeighborhoodGenerator() {
	}

	@Override
	public void start(IResolver resolver) throws Exception {
	}

	@Override
	public void stop() throws Exception {
	}

	@Override
	public int size() {
		return -1;
	}

	@Override
	public IndexedNeighborGraph subgraph(Integer node) {
		return new CompleteGraph(node);
	}

	@Override
	public int[] verticesOf(Integer node) {
		int[] vertices = new int[node];
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = i + 1;
		}
		return vertices;
	}

	@Override
	public String id() {
		return "NeighborhoodLoader";
	}

}
