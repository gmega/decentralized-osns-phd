package it.unitn.disi.graph.cli;

import it.unitn.disi.graph.BFSIterable.BFSIterator;
import it.unitn.disi.graph.GraphWriter;
import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.collections.Pair;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class SnowballSample extends GraphAnalyzer {

	private final int fSeed;
	
	private final int fOrder;

	public SnowballSample(@Attribute("decoder") String decoder,
			@Attribute("order") int order,
			@Attribute(value = "seed", defaultValue = "-1") int seed) {
		super(decoder);
		fSeed = seed;
		fOrder = order;
	}

	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream stream)
			throws IOException {
		int seed = fSeed >= 0 ? fSeed : new Random().nextInt(graph.size());
		BFSIterator it = new BFSIterator(graph, seed);
		ArrayList<Integer> vertices = new ArrayList<Integer>();
		while (it.hasNext()) {
			Pair<Integer, Integer> element = it.next();
			
			int vertex = element.a;
			int depth = element.b;
			
			if (depth > fOrder) {
				break;
			}
			
			vertices.add(vertex);
		}
		
		SubgraphDecorator decorator = new SubgraphDecorator(graph, false);
		decorator.setVertexList(vertices);
		GraphWriter.printAdjList(decorator, decorator, new OutputStreamWriter(stream));	
	}
}
