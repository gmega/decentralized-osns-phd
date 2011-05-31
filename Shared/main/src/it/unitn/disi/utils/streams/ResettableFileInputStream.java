package it.unitn.disi.utils.streams;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResettableFileInputStream extends InputStream {

	private File fFile;
	private BufferedInputStream fDelegate;

	public ResettableFileInputStream(File file) throws IOException {
		fFile = file;
		fDelegate = new BufferedInputStream(new FileInputStream(file));
	}

	@Override
	public int available() throws IOException {
		return fDelegate.available();
	}

	@Override
	public void close() throws IOException {
		fDelegate.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		fDelegate.mark(readlimit);
	}

	@Override
	public boolean markSupported() {
		return fDelegate.markSupported();
	}

	@Override
	public int read() throws IOException {
		return fDelegate.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return fDelegate.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return fDelegate.read(b);
	}

	@Override
	public synchronized void reset() throws IOException {
		fDelegate.close();
		fDelegate = new BufferedInputStream(new FileInputStream(fFile));
	}

	@Override
	public long skip(long n) throws IOException {
		return fDelegate.skip(n);
	}

}
