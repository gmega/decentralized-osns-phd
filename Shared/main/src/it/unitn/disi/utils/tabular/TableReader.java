package it.unitn.disi.utils.tabular;

import it.unitn.disi.utils.exception.ParseException;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Utility class which allows tabular data to be read in a simple way.
 * 
 * @author giuliano
 */
public class TableReader {

	public static final String FS = " ";

	private final ILineReader fReader;

	private final ArrayList<String> fHeader;

	private final List<String> fROHeader;

	private String[] fCurrent;

	private String fNext;

	private int fRow = -1;

	/**
	 * Attaches a {@link TableReader} to an input stream.
	 * 
	 * @param stream
	 *            stream containing table data.
	 * 
	 * @throws IOException
	 */
	public TableReader(InputStream stream) throws IOException {
		this(new InputStreamLR(stream));
	}

	public TableReader(ILineReader reader) throws IOException {
		fReader = reader;
		fHeader = readHeader();
		fROHeader = Collections.unmodifiableList(fHeader);
	}

	private TableReader(ILineReader reader, String[] header, String next) {
		fReader = reader;
		fHeader = new ArrayList<String>();
		for (String key : header) {
			fHeader.add(key);
		}
		fROHeader = Collections.unmodifiableList(fHeader);
		fNext = next;
	}

	public void streamRepositioned() throws IOException {
		fReader.flushBuffers();
		readLine(); // Buffers the next line, if any.
	}

	public List<String> columns() {
		return fROHeader;
	}

	public boolean hasNext() {
		return fNext != null;
	}

	public void next() throws IOException {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
		readLine();
		if (fCurrent.length != fHeader.size()) {
			throw new ParseException("Table width changed.");
		}
	}

	public String get(String key) {
		int idx = fHeader.indexOf(key);
		if (idx == -1) {
			return null;
		}
		return fCurrent[idx];
	}

	public void close() throws IOException {
		fReader.close();
	}

	public int currentRow() {
		return fRow;
	}

	public TableReader fromCurrentRow() {
		return new TableReader(fReader, fCurrent, fNext);
	}

	private ArrayList<String> readHeader() throws IOException {
		// Fills in the readahead slot.
		readLine();

		// Reads the first line.
		ArrayList<String> header = new ArrayList<String>();
		String[] headerParts = readLine();

		for (int i = 0; i < headerParts.length; i++) {
			if (isInt(headerParts[i])) {
				return defaultHeader(headerParts.length);
			}
			header.add(headerParts[i]);
		}
		return header;
	}

	private ArrayList<String> defaultHeader(int length) {
		ArrayList<String> header = new ArrayList<String>();
		for (int i = 0; i < length; i++) {
			header.add("V" + i);
		}
		return header;
	}

	private String[] readLine() throws IOException {
		if (fNext != null) {
			fCurrent = fNext.split(FS);
		}
		fNext = fReader.readLine();
		fRow++;
		return fCurrent;
	}

	private boolean isInt(String string) {
		try {
			Integer.parseInt(string);
			return true;
		} catch (NumberFormatException ex) {
			return false;
		}
	}

	// -------------------------------------------------------------------------
	// Extra interfaces to make it more extensible.
	// -------------------------------------------------------------------------

	public static interface ILineReader extends Closeable {
		/**
		 * @return the next line in the source, or <code>null</code> if there
		 *         are no more lines to read.
		 * 
		 * @throws IOException
		 */
		public String readLine() throws IOException;

		/**
		 * Flushes all read buffers, and starts reading from the underlying
		 * stream again.
		 * 
		 * @throws IOException
		 */
		public void flushBuffers() throws IOException;
	}

	private static class InputStreamLR implements ILineReader {

		private BufferedReader fReader;

		private InputStream fStream;

		public InputStreamLR(InputStream is) {
			fStream = is;
			flushBuffers();
		}

		@Override
		public void close() throws IOException {
			fReader.close();
		}

		@Override
		public String readLine() throws IOException {
			return fReader.readLine();
		}

		@Override
		public void flushBuffers() {
			fReader = new BufferedReader(new InputStreamReader(fStream));
		}
	}

}
