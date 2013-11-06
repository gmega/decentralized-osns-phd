package it.unitn.disi.utils.streams;

import java.io.IOException;
import java.io.OutputStream;

public class TeeOutputStream extends OutputStream {

	private final OutputStream fO1;

	private final OutputStream fO2;

	private Exception fLast;

	public TeeOutputStream(OutputStream o1, OutputStream o2) {
		fO1 = o1;
		fO2 = o2;
	}

	@Override
	public void write(int b) throws IOException {
		fO1.write(b);
		fO2.write(b);
	}

	@Override
	public void flush() throws IOException {
		safeFlush(fO1);
		safeFlush(fO2);
		throwException();
	}

	@Override
	public void close() throws IOException {
		safeClose(fO1);
		safeClose(fO2);
		throwException();
	}

	private void safeClose(OutputStream stream) {
		try {
			stream.close();
		} catch (Exception ex) {
			fLast = ex;
		}
	}

	private void safeFlush(OutputStream stream) {
		try {
			stream.flush();
		} catch (Exception ex) {
			fLast = ex;
		}
	}

	private void throwException() throws IOException {
		if (fLast != null) {
			Exception last = fLast;
			fLast = null;
			if (last instanceof IOException) {
				throw (IOException) last;
			} else {
				throw (RuntimeException) last;
			}
		}
	}
}
