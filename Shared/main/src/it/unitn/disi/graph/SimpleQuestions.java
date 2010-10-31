package it.unitn.disi.graph;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.GraphCodecHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.BitSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Answers two simple questions about a graph: whether it is simple, and whether
 * it is directed.
 * 
 * @author giuliano
 */
@AutoConfig
public class SimpleQuestions implements ITransformer {

	@Attribute(value="decoder", defaultValue="it.unitn.disi.graph.codecs.ByteGraphDecoder")
	private String decoder;
	
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		LightweightStaticGraph graph = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(is, this.decoder));
		PrintStream p = new PrintStream(oup);
		
		BitSet set = new BitSet(graph.size());
		
		boolean directed = false;
		boolean simple = true;
		int edges = 0;
		for (int i = 0; i < graph.size(); i++) {
			set.clear();
			for (int j = 0; j < graph.degree(i); j++) {
				edges++;
				int neighbor = graph.getNeighbor(i, j);
				if (!graph.isEdge(neighbor, i)) {
					directed = true;
				}
				if (set.get(neighbor)) {
					simple = false;
				}
				set.set(neighbor);
			}
		}
		
		if (simple) {
			p.println("Simple.");
		} else {
			p.println("Non-simple.");
		}
		
		if(directed) {
			p.println("Directed.");
		} else {
			p.println("Undirected.");
		}
		
		System.out.println("Edges (directed) " + edges+".");
	}

}
