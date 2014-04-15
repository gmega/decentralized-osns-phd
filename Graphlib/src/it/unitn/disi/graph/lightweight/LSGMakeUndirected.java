package it.unitn.disi.graph.lightweight;

import it.unitn.disi.graph.IGraphVisitor;

public class LSGMakeUndirected extends LSGTransformer {

	@Override
	protected void graphLoop(IGraphVisitor action) throws Exception {
		LightweightStaticGraph lsg = this.sourceGraph();
		for (int i = 0; i < lsg.size(); i++) {
			int [] neighbors = lsg.fastGetNeighbours(i);
			for(int j = 0; j < neighbors.length; j++) {
				int neighbor = neighbors[j];
				action.visitEdge(i, neighbor);
				if (!lsg.isEdge(neighbor, i)) {
					action.visitEdge(neighbor, i);
				}
			}
		}
	}
}
