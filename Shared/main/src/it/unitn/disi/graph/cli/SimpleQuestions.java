package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.BitSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Answers three simple questions about a graph: whether it is simple, and whether
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
				
		p.println(graph.isSimple() ? "Simple." : "Non-simple.");
		p.println(graph.directed() ? "Directed." : "Undirected.");
		p.println(graph.isConnected() ? "Connected." : "Disconnected.");
		
		System.out.println("Edges (directed) " + graph.edgeCount() + ".");
	}

}
