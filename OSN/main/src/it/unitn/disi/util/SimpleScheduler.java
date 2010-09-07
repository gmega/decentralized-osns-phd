package it.unitn.disi.util;

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
	}

	@Override
	public Integer next() {
		chkHasNext();
		Integer next = fCurrent;
		fCurrent = fTok.hasMoreElements() ? Integer.parseInt(fTok.nextToken()) : null;
		return next;
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
