package it.unitn.disi.graph.codecs;

import it.unitn.disi.utils.IDMapper;

import java.io.IOException;
import java.io.OutputStream;

import peersim.graph.Graph;

/**
 * Abstract encoder for edgelist based formats (.el, .bin).
 * 
 * @author giuliano
 */
public abstract class AbstractEdgeListEncoder implements GraphEncoder {

	protected final OutputStream fStream;

	public AbstractEdgeListEncoder(OutputStream stream) {
		fStream = stream;
	}

	@Override
	public void encode(Graph g, IDMapper mapper) throws IOException {
		int size = g.size();
		for (int i = 0; i < size; i++) {
			for (Integer neighbor : g.getNeighbours(i)) {
				encodePair(mapper.map(i), mapper.map(neighbor), fStream);
			}
		}
	}

	@Override
	public boolean supportsTranscoding() {
		return true;
	}

	@Override
	public void transcode(GraphDecoder decoder, IDMapper mapper)
			throws IOException {
		while (decoder.hasNext()) {
			encodePair(mapper.map(decoder.getSource()),
					mapper.map(decoder.next()), fStream);
		}
	}

	protected abstract void encodePair(int source, int target,
			OutputStream stream) throws IOException;
}
