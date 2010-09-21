package it.unitn.disi.utils.collections;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A /dev/null counterpart with an {@link OutputStream} interface.
 * 
 * @author giuliano
 */
public class NullOutputStream extends OutputStream {
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
	}

	@Override
	public void write(byte[] b) throws IOException {
	}

	@Override
	public void write(int b) throws IOException {
	}
}
