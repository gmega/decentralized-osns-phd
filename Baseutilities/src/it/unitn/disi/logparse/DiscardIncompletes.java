package it.unitn.disi.logparse;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPInputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class DiscardIncompletes implements IMultiTransformer {

	private final String fEndPrefix;

	public DiscardIncompletes(@Attribute("endprefix") String endPrefix) {
		fEndPrefix = endPrefix;
	}

	@Override
	public void execute(StreamProvider provider) throws Exception {
		for (int i = 0; i < provider.inputStreams(); i++) {
			System.err.println("File " + i + " of " + provider.inputStreams() + ".");
			parse(provider.input(i), provider.output(0),
					provider.isInputGZipped());
		}
	}

	private void parse(InputStream is, OutputStream oup, boolean gzipped)
			throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input(
				is, gzipped)));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oup));
		
		System.err.println("-- 1. Find last matching character.");
		
		char[] matchArray = buildMatchArray();
		long lastChar = establishLastMatching(reader, matchArray);

		System.err.println("-- 2. Print until last matching character.");
		// Restarts the reading.
		is.reset();
		reader = new BufferedReader(new InputStreamReader(input(is, gzipped)));
		for (long i = 0; i <= lastChar; i++) {
			writer.write(safeRead(reader));
		}
		writer.flush();
	}

	private InputStream input(InputStream is, boolean gzipped)
			throws IOException {
		return gzipped ? new GZIPInputStream(is) : is;
	}

	private long establishLastMatching(BufferedReader reader, char[] matchArray)
			throws IOException {
		long totalRead = 0;
		long lastMatch = -1;

		int mIndex = 0;
		int c;
		while ((c = safeRead(reader)) != -1) {
			totalRead++;
			// Matches the next character.
			if (c == matchArray[mIndex]) {
				mIndex++;
				if (mIndex == matchArray.length) {
					lastMatch = totalRead;
					mIndex = 0;
				}
			}
			// Doesn't match the next character.
			else {
				mIndex = 0;
			}
		}

		return lastMatch;
	}

	private char[] buildMatchArray() {
		String separator = System.getProperty("line.separator");
		if (separator == null) {
			System.err.println("ERROR: line separator not defined.");
		}

		char[] matching = new char[separator.length() + fEndPrefix.length()];
		System.arraycopy(separator.toCharArray(), 0, matching, 0,
				separator.length());
		System.arraycopy(fEndPrefix.toCharArray(), 0, matching,
				separator.length(), fEndPrefix.length());
		return matching;
	}

	private int safeRead(BufferedReader reader) throws IOException {
		try {
			return reader.read();
		} catch (EOFException ex) {
			return -1;
		}
	}

}
