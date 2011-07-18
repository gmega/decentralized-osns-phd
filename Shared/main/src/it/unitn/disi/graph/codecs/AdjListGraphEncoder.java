package it.unitn.disi.graph.codecs;

import it.unitn.disi.utils.IDMapper;

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
	public void encode(Graph g, IDMapper mapper) throws IOException {
		for (int i = 0; i < g.size(); i++) {
			StringBuffer line = new StringBuffer();
			line.append(mapper.map(i));
			
			for (Integer neighbor : g.getNeighbours(i)) {
				line.append(" ");
				line.append(mapper.map(neighbor));
			}

			fStream.println(line.toString());
		}
	}

	@Override
	public boolean supportsTranscoding() {
		return false;
	}

	@Override
	public void transcode(GraphDecoder decoder, IDMapper mapper) throws IOException {
		throw new UnsupportedOperationException();
	}

}
