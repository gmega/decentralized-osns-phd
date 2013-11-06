package it.unitn.disi.graph.lightweight;

import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.DenseIDMapper;
import it.unitn.disi.utils.SparseIDMapper;
import peersim.graph.Graph;

public class LSGCreateSubgraph extends LSGTransformer {

	private final int[] fVertices;

	private AbstractIDMapper fMapper;

	public LSGCreateSubgraph(int[] vertices) {
		fVertices = vertices;
	}

	@Override
	protected void graphLoop(Action action) throws Exception {
		init();
		LightweightStaticGraph graph = sourceGraph();
		for (int vertex : fVertices) {
			int source = fMapper.map(vertex);
			action.addVertex(source);
			
			int degree = graph.degree(vertex);
			for (int i = 0; i < degree; i++) {
				int neighbor = graph.getNeighbor(vertex, i);
				if (!fMapper.isMapped(neighbor)) {
					continue;
				}
				int mappedNeighbor = fMapper.map(neighbor);
				action.addVertex(mappedNeighbor);
				action.edge(source, mappedNeighbor);
			}
		}
	}

	private void init() {
		Graph source = sourceGraph();
		if (source.size() / 2 <= fVertices.length) {
			fMapper = new DenseIDMapper(fVertices.length);
		} else {
			fMapper = new SparseIDMapper();
		}

		for (int vertex : fVertices) {
			fMapper.addMapping(vertex);
		}
	}

}
