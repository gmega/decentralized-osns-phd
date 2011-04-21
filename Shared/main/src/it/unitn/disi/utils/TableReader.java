package it.unitn.disi.utils;

import it.unitn.disi.utils.exception.ParseException;

import java.io.BufferedReader;
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

	private final BufferedReader fReader;

	private final ArrayList<String> fHeader;

	private final List<String> fROHeader;

	private String[] fCurrent;

	private String fNext;

	/**
	 * Attaches a {@link TableReader} to an input stream.
	 * 
	 * @param stream
	 *            stream containing table data.
	 * 
	 * @throws IOException
	 */
	public TableReader(InputStream stream) throws IOException {
		fReader = new BufferedReader(new InputStreamReader(stream));
		fHeader = readHeader();
		fROHeader = Collections.unmodifiableList(fHeader);
	}

	private TableReader(BufferedReader reader, String[] header, String next) {
		fReader = reader;
		fHeader = new ArrayList<String>();
		for (String key : header) {
			fHeader.add(key);
		}
		fROHeader = Collections.unmodifiableList(fHeader);
		fNext = next;
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
}
