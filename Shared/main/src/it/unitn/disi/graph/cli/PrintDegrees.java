package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import peersim.config.AutoConfig;
import peersim.graph.Graph;

@AutoConfig
public class PrintDegrees implements ITransformer {

	public void execute(InputStream is, OutputStream oup) throws IOException {
		ByteGraphDecoder dec = new ByteGraphDecoder(is);
		Graph graph = LightweightStaticGraph.load(dec);

		OutputStreamWriter writer = new OutputStreamWriter(oup);

		try {
			for (int i = 0; i < graph.size(); i++) {
				writer.write(i + " " + graph.degree(i) + "\n");
			}
		} finally {
			writer.close();
		}
	}
}
