package it.unitn.disi.cli;

import it.unitn.disi.codecs.ByteGraphDecoder;
import it.unitn.disi.utils.graph.LightweightStaticGraph;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.AutoConfig;

@AutoConfig
public class IsUndirected implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		LightweightStaticGraph graph = LightweightStaticGraph
				.load(new ByteGraphDecoder(is));
		PrintStream p = new PrintStream(oup);

		for (int i = 0; i < graph.size(); i++) {
			for (int j = 0; j < graph.degree(i); j++) {
				if (!graph.isEdge(j, i)) {
					p.println("Directed.");
				}
			}
		}
		
		p.println("Undirected.");
	}

}
