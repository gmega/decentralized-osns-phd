package it.unitn.disi.graph.lightweight;

public class LSGMakeUndirected extends LSGTransformer {

	@Override
	protected void graphLoop(Action action) throws Exception {
		LightweightStaticGraph lsg = this.sourceGraph();
		for (int i = 0; i < lsg.size(); i++) {
			int [] neighbors = lsg.fastGetNeighbours(i);
			for(int j = 0; j < neighbors.length; j++) {
				int neighbor = neighbors[j];
				action.edge(i, neighbor);
				if (!lsg.isEdge(neighbor, i)) {
					action.edge(neighbor, i);
				}
			}
		}
	}
}
