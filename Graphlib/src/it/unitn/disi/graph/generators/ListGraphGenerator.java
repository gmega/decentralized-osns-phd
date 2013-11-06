package it.unitn.disi.graph.generators;

import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LSGCreator;

public class ListGraphGenerator implements IGraphProvider {

	@Override
	public int size() {
		return -1;
	}

	@Override
	public IndexedNeighborGraph subgraph(Integer node) {
		LinearCreator lc = new LinearCreator(node);
		return lc.create(true);
	}

	@Override
	public int[] verticesOf(Integer node) {
		int [] vertices = new int[node];
		for (int i = 0; i < node; i++) {
			vertices[i] = i;
		}
		return vertices;
	}
	
	static class LinearCreator extends LSGCreator {
		
		private int fSize;
		
		public LinearCreator(int size) {
			fSize = size;
		}

		@Override
		protected void graphLoop(Action action) throws Exception {
			for (int i = 0; i < fSize - 1; i++) {
				action.edge(i, i + 1);
				action.edge(i + 1, i);
			}
		}
	}
}