package it.unitn.disi.graph.codecs;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;


/**
 * {@link AbstractEdgeListDecoder} provides an iterator-like interface to access to 
 * graphs encoded as edge lists.<BR>
 * <BR>
 * 
 * @author giuliano
 */
public abstract class AbstractEdgeListDecoder implements ResettableGraphDecoder {

	private boolean fSeenEof;

	private int fSource;
	
	private InputStream fIs;

	public AbstractEdgeListDecoder(InputStream is) {
		fIs = is;
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
	
	final protected void init() throws IOException {
		fSource = readInt(true);
	}
	
	final protected InputStream inputStream() {
		return fIs;
	}
	
	final protected void eofSeen() {
		fSeenEof = true;
	}

	protected abstract int readInt(boolean eofAllowed) throws IOException;
	
	protected abstract void inputStreamReset(InputStream is) throws IOException;

	public int getSource() {
		return fSource;
	}

	public void reset() throws IOException {
		fIs.reset();
		fSeenEof = false;
		inputStreamReset(fIs);
		fSource = readInt(true);
	}
	
	public void realign() throws IOException {
		init();
	}

	public void unexpectedEOF(String msg) throws IOException {
		throw new IOException("Unexpected end-of-file (" + msg + ").");
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}