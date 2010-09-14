package it.unitn.disi.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import com.google.common.collect.PeekingIterator;

public class SimpleScheduler implements PeekingIterator<Integer>{
	
	private StringTokenizer fTok;
	
	private Integer fCurrent;
		
	public static SimpleScheduler fromFile(File file) throws IOException {
		final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
		final byte [] bytes = new byte[(int) file.length()];
		bis.read(bytes);
		bis.close();
		return new SimpleScheduler(new String(bytes));
	}
	
	public SimpleScheduler (String string) {
		fTok = new StringTokenizer(string);
		advance();
	}

	@Override
	public Integer next() {
		chkHasNext();
		Integer next = fCurrent;
		advance();
		return next;
	}
	
	private void advance() {
		fCurrent = fTok.hasMoreElements() ? Integer.parseInt(fTok.nextToken()) : null;
	}
	
	@Override
	public Integer peek() {
		chkHasNext();
		return fCurrent;
	}

	private void chkHasNext() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}
	
	@Override
	public boolean hasNext() {
		return fCurrent != null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
