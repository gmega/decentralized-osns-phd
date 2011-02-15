package it.unitn.disi.graph.codecs;

import java.io.IOException;

public interface ResettableGraphDecoder extends GraphDecoder {
	/**
	 * Resets the iterator to the initial position.
	 * 
	 * @throws IOException
	 *             if that fails due to an I/O error.
	 */
	public void reset() throws IOException;

	/**
	 * Optional operation. To be called by clients when the underlying I/O
	 * streams get shifted between two consecutive calls to {@link #next()}. <BR>
	 * Note that not all decoders can recover from arbitrary shifts (most in
	 * fact cannot), so refer to the documentation of the decoder implementation
	 * to understand what you can an cannot do.
	 * 
	 * @throws UnsupportedOperationException
	 *             if the operation is not supported.
	 */
	public void realign() throws IOException;
}
