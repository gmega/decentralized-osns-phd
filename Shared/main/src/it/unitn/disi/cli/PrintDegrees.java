package it.unitn.disi.cli;

import it.unitn.disi.codecs.ByteGraphDecoder;
import it.unitn.disi.utils.graph.LightweightStaticGraph;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import peersim.graph.Graph;

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
