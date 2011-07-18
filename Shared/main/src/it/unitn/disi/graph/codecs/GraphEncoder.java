package it.unitn.disi.graph.codecs;

import it.unitn.disi.utils.IDMapper;

import java.io.IOException;

import peersim.graph.Graph;

public interface GraphEncoder {

	/**
	 * Encodes a graph.
	 * 
	 * @param g
	 *            the graph to encode.
	 */
	public void encode(Graph g, IDMapper mapper) throws IOException;

	/**
	 * @return <code>true</code> if this encoder supports
	 *         {@link #transcode(GraphDecoder)}, or <code>false</code>
	 *         otherwise.
	 */
	public boolean supportsTranscoding();

	/**
	 * Optional operation. Recodes a graph directly from a {@link GraphDecoder},
	 * usually without loading the contents into memory.
	 * 
	 * @param decoder
	 *            the source decoder.
	 * 
	 * @throws UnsupportedOperationException
	 *             if {@link #supportsTranscoding()} returns <code>false</code>.
	 */
	public void transcode(GraphDecoder decoder, IDMapper mapper)
			throws IOException;
}
