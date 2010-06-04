package it.unitn.disi.codecs;

import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

/**
 * {@link ByteGraphDecoder} provides an iterator-like interface to access of
 * binary-encoded graphs.<BR>
 * <BR>
 * <b>Format:</b> a concatenated stream of <source, target> pairs, where each
 * pair is a 32 bit {@link Integer}.
 * 
 * @author giuliano
 */
public class ByteGraphDecoder implements ResettableGraphDecoder {

	private InputStream fIs;

	private boolean fSeenEof;

	private int fSource;

	private byte[] fBuf = new byte[4];

	public ByteGraphDecoder(InputStream is) throws IOException {
		fIs = is;
		fSource = readInt(true);
	}

	public boolean hasNext() {
		return !fSeenEof;
	}

	public Integer next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		return advance();
	}

	private int advance() {
		try {
			int toReturn = readInt(false);
			fSource = readInt(true);
			return toReturn;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private int readInt(boolean eofAllowed) throws IOException {

		int read = fIs.read(fBuf);
		if (read == -1) {
			fSeenEof = true;
			if (!eofAllowed) {
				unexpectedEOF("odd number of integers (encoding error)");
			}
			return -1;
		} else if (read != fBuf.length) {
			unexpectedEOF("file size is not multiple of " + Integer.SIZE);
		}

		return CodecUtils.decodeInt(fBuf);
	}

	public int getSource() {
		return fSource;
	}

	public void reset() throws IOException {
		fIs.reset();
		fSeenEof = false;
		fSource = readInt(true);
	}

	public void unexpectedEOF(String msg) throws IOException {
		throw new IOException("Unexpected end-of-file (" + msg + ").");
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
