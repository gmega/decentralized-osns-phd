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
public class ByteGraphDecoder extends AbstractEdgeListDecoder {

	private byte[] fBuf = new byte[4];

	public ByteGraphDecoder(InputStream is) throws IOException {
		super(is);
		init();
	}

	@Override
	protected int readInt(boolean eofAllowed) throws IOException {

		int read = inputStream().read(fBuf);
		if (read == -1) {
			eofSeen();
			if (!eofAllowed) {
				unexpectedEOF("odd number of integers (encoding error)");
			}
			return -1;
		} else if (read != fBuf.length) {
			unexpectedEOF("file size is not multiple of " + Integer.SIZE);
		}

		return CodecUtils.decodeInt(fBuf);
	}
}
