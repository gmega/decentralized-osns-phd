package it.unitn.disi.codecs;

import java.io.IOException;

public interface ResettableGraphDecoder extends GraphDecoder {
	public void reset() throws IOException;
}
