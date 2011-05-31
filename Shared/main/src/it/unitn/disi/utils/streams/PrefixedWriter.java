package it.unitn.disi.utils.streams;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Simple {@link Writer} subclass which appends a prefix at the beginning of
 * each line. Should be used with a {@link Writer} implementation which supports
 * efficient single character writes (e.g. efficient {@link Writer#write(int)}),
 * such as a {@link BufferedWriter}.
 * 
 * @author giuliano
 */
public class PrefixedWriter extends Writer {

	private final String fPrefix;

	private final char fNewline;

	private Writer fOutput;

	private boolean fShouldWritePrefix = true;

	public PrefixedWriter(String prefix, Writer oWriter) {
		fPrefix = prefix;
		fOutput = oWriter;
		fNewline = System.getProperty("line.separator").charAt(0);
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		int last = off + len;
		for (int i = off; i < last; i++) {
			writePrefix();
			if (cbuf[i] == fNewline) {
				shouldWritePrefix();
			}
			fOutput.write(cbuf[i]);
		}
	}

	@Override
	public void flush() throws IOException {
		fOutput.flush();
	}

	@Override
	public void close() throws IOException {
		fOutput.close();
	}

	private void writePrefix() throws IOException {
		if (fShouldWritePrefix) {
			fOutput.write(fPrefix);
			fShouldWritePrefix = false;
		}
	}

	private void shouldWritePrefix() {
		fShouldWritePrefix = true;
	}
}
