package it.unitn.disi.utils.streams;

import java.io.IOException;
import java.io.InputStream;

public class SafeBufferReader {

	private final InputStream fStream;

	private boolean fEOF;

	public SafeBufferReader(InputStream stream) {
		fStream = stream;
	}

	public boolean eof() {
		return fEOF;
	}

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
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

		}

		return totalRead;
	}
}
