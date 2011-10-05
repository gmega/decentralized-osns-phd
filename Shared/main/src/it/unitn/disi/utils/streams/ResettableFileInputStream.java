package it.unitn.disi.utils.streams;

import it.unitn.disi.utils.MiscUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decorator adding mark/reset support to {@link FileInputStream}, as well as
 * read buffering.
 * 
 * @author giuliano
 */
public class ResettableFileInputStream extends InputStream {

	private FileInputStream fStream;

	private BufferedInputStream fBuffer;

	private long fMark = 0;

	public ResettableFileInputStream(File file) throws IOException {
		fStream = new FileInputStream(file);
		fBuffer = new BufferedInputStream(fStream);
	}

	@Override
	public int available() throws IOException {
		return fBuffer.available();
	}

	@Override
	public void close() throws IOException {
		fBuffer.close();
	}

	@Override
	public synchronized void mark(int readlimit) {
		try {
			long read = fStream.getChannel().position();
			int buffered = fBuffer.available() - fStream.available();
			fMark = read - buffered;
		} catch (IOException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public int read() throws IOException {
		return fBuffer.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		return fBuffer.read(b, off, len);
	}

	@Override
	public int read(byte[] b) throws IOException {
		return fBuffer.read(b);
	}

	@Override
	public synchronized void reset() throws IOException {
		reposition(fMark);
	}
	
	public synchronized void fromZero() throws IOException {
		reposition(0);
	}

	private void reposition(long offset) throws IOException {
		// To reposition, we need to reposition the FileInputStream
		// and throw away the buffer.
		fStream.getChannel().position(offset);
		fBuffer = new BufferedInputStream(fStream);
	}

	@Override
	public long skip(long n) throws IOException {
		return fBuffer.skip(n);
	}

}
