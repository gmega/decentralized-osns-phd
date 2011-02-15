package it.unitn.disi.graph.codecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class AdjListGraphDecoder implements ResettableGraphDecoder {

	private InputStream fIs;
	
	private LineNumberReader fReader;

	private StringTokenizer fTokenizer;

	private Integer fSource;

	public AdjListGraphDecoder(InputStream is) throws IOException {
		fIs = is;
		init();
	}

	public boolean hasNext() {
		if (!fTokenizer.hasMoreTokens()) {
			try {
				advanceLine();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		return fTokenizer.hasMoreTokens();
	}

	private void advanceLine() throws IOException {
		String line;
		do {
			line = fReader.readLine();
			if (line == null) {
				return;
			} else if (line.startsWith("#")) {
				continue;
			}
			fTokenizer = new StringTokenizer(line);
			fSource = Integer.parseInt(fTokenizer.nextToken());
		} while (!fTokenizer.hasMoreTokens());
	}
	
	public void realign() throws IOException {
		// We cannot recover from arbitrary realignments.
		advanceLine();
	}

	public int getSource() {
		return fSource;
	}

	public Integer next() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}

		return Integer.parseInt(fTokenizer.nextToken());
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public void reset() throws IOException {
		fIs.reset();
		init();
	}
	
	private void init() throws IOException {
		fReader = new LineNumberReader(new BufferedReader(
				new InputStreamReader(fIs)));
		fTokenizer = null;
		fSource = -1;
		advanceLine();
	}
}
