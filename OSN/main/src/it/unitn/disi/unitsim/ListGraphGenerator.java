package it.unitn.disi.unitsim;

import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.lightweight.LSGCreator;

@AutoConfig
public class ListGraphGenerator implements IGraphProvider, IPlugin {

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

	@Override
	public String id() {
		return "NeighborhoodLoader";
	}

	@Override
	public void start(IResolver resolver) throws Exception {
	}

	@Override
	public void stop() throws Exception {
	}

}
