package it.unitn.disi.graph.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

import peersim.config.Attribute;

import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

public class EgonetResilience extends GraphAnalyzer {
	
	public EgonetResilience(@Attribute("decoder") String decoder) {
		super(decoder);
	}

	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream stream)
			throws IOException {
		
		SubgraphDecorator egonet = new SubgraphDecorator(graph, true);
		
		// For each egonet...
		for (int i = 0; i < graph.size(); i++) {
			HashSet<Integer> egovertices = toSet(graph.fastGetNeighbours(i));
			egovertices.add(i);
			egonet.setVertexList(egovertices);
			// ... while it remains connected ...
			while (GraphAlgorithms.isConnected(egonet) && egovertices.size() != 0) {
				// ... strips the highest degree vertex.
				int id = findMaxDegree(egonet);
				egovertices.remove(id);
				egonet.setVertexList(egovertices);
			}
		}
	}
	
	private int findMaxDegree(SubgraphDecorator egonet) {
		int size = egonet.size();
		int maxDegree = Integer.MIN_VALUE;
		int idx = Integer.MIN_VALUE;
		for (int i = 0; i < size; i++) {
			int degree = egonet.degree(i);
			if (degree > maxDegree) {
				maxDegree = degree;
				idx = i;
			}
		}
		
		return egonet.inverseIdOf(idx);
	}
	                                                     
	private HashSet<Integer> toSet(int [] array) {
		HashSet<Integer> set = new HashSet<Integer>();
		for (int i = 0; i < array.length; i++) {
			set.add(array[i]);
		}
		return set;
	}

}
