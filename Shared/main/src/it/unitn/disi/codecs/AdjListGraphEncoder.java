package it.unitn.disi.codecs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.graph.Graph;

public class AdjListGraphEncoder implements GraphEncoder {
	
	private final PrintStream fStream;
	
	public AdjListGraphEncoder(OutputStream stream) {
		fStream = new PrintStream(stream);
	}

	@Override
	public void encode(Graph g) throws IOException {
		for (int i = 0; i < g.size(); i++) {
			StringBuffer line = new StringBuffer();
			line.append(i);
			
			for (Integer neighbor : g.getNeighbours(i)) {
				line.append(" ");
				line.append(neighbor);
			}

			fStream.println(line.toString());
		}
	}

	@Override
	public boolean supportsTranscoding() {
		return false;
	}

	@Override
	public void transcode(GraphDecoder decoder) throws IOException {
		throw new UnsupportedOperationException();
	}

}
