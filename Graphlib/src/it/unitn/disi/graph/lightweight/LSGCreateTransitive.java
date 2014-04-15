package it.unitn.disi.graph.lightweight;

import it.unitn.disi.graph.IGraphVisitor;
import it.unitn.disi.graph.BFSIterable.BFSIterator;
import it.unitn.disi.utils.collections.Pair;

public class LSGCreateTransitive extends LSGTransformer {

	private final int fOrder;
	
	public LSGCreateTransitive(int order) {
		fOrder = order;
	}
	
	@Override
	protected void graphLoop(IGraphVisitor action) throws Exception {
		LightweightStaticGraph base = this.sourceGraph();
		for (int i = 0; i < base.size(); i++) {
			BFSIterator it = new BFSIterator(base, i);
			// Skips the root.
			it.next();
			while (it.hasNext()) {
				// "a" is the node ID, "b" is the distance from the root.
				Pair<Integer, Integer> next = it.next();
				if (next.b > fOrder) {
					break;
				}
				action.visitEdge(i, next.a);
			}
		}
	}

}
