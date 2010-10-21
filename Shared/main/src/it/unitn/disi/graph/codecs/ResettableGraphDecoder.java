package it.unitn.disi.graph.codecs;

import java.io.IOException;

public interface ResettableGraphDecoder extends GraphDecoder {
	public void reset() throws IOException;
}
