package it.unitn.disi.churn;

import it.unitn.disi.utils.tabular.TableReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;

public abstract class SequentialAttributeReader<K> {

	private String fIdKey;

	private String fCurrentRoot;

	private TableReader fReader;
	
	private boolean fChanged;
	
	private boolean fDone;
	
	public SequentialAttributeReader(InputStream stream, String id)
			throws IOException {
		fIdKey = id;
		fReader = new TableReader(stream);
		fReader.next();
		fCurrentRoot = fReader.get(fIdKey);
	}

	public boolean hasNext() {
		return fReader.hasNext();
	}

	public String currentRoot() {
		checkNotDone();
		return fCurrentRoot;
	}

	public void skipCurrent() throws IOException {
		checkNotDone();
		String root;
		while ((root = fReader.get(fIdKey)).equals(fCurrentRoot)
				&& fReader.hasNext()) {
			fReader.next();
		}
		fCurrentRoot = root;
	}
	
	private void checkNotDone() {
		if (fDone) {
			throw new IllegalStateException();
		}
	}

	public abstract K read(int [] ids) throws IOException;
	
	protected String get(String key) {
		checkNotDone();
		return fReader.get(key);
	}

	protected void advance() throws IOException {
		if (!fReader.hasNext()) {
			fChanged = true;
			fDone = true;
			return;
		}
		
		fReader.next();
		String root = fReader.get(fIdKey);
		if (!root.equals(fCurrentRoot)) {
			fCurrentRoot = root;
			fChanged = true;
		}
	}
	
	protected boolean rootChanged() {
		if (fChanged) {
			fChanged = false;
			return true;
		}
		return false;
	}

	protected int idOf(int id, int[] ids) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == id) {
				return i;
			}
		}

		throw new NoSuchElementException();
	}

}
