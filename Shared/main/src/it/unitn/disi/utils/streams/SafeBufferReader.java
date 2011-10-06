package it.unitn.disi.utils.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link SafeBufferReader} is a simple utility class encapsulating a strategy
 * for working around a quirk in some {@link InputStream} implementations,
 * wherein {@link InputStream#read()} returns a smaller amount of bytes than
 * what has been requested, even if the EOF isn't yet about to be reached.
 * 
 * @author giuliano
 */
public class SafeBufferReader {
	
	private static final int DEFAULT_STAGGER = 100;

	private final InputStream fStream;
	
	private final int fStagger;

	private boolean fEOF;
	
	public SafeBufferReader(InputStream stream) {
		this(stream, DEFAULT_STAGGER);
	}

	public SafeBufferReader(InputStream stream, int stagger) {
		fStream = stream;
		fStagger = stagger;
	}

	public boolean eof() {
		return fEOF;
	}

	/**
	 * Fills in a buffer, returning only when we're sure that either the buffer
	 * has been filled, or an EOF has been seen.
	 * 
	 * @param buffer
	 *            the buffer to be filled.
	 * 
	 * @param length
	 *            the number of bytes to be read.
	 * @return the number of bytes actually read.
	 * @throws IOException
	 *             if the {@link InputStream} coughs it up while we try to read
	 *             from it.
	 */
	public int fillBuffer(byte[] buffer, int length) throws IOException {
		int totalRead = 0;

		while (true) {
			int read;
			try {
				read = fStream.read(buffer, totalRead, length - totalRead);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			if (read == -1) {
				fEOF = true;
				break;
			} else {
				totalRead += read;
			}

			if (totalRead == length) {
				break;
			}

			try {
				Thread.sleep(fStagger);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		}

		return totalRead;
	}
}
