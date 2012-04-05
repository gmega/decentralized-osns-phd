package it.unitn.disi.churn.connectivity.p2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import it.unitn.disi.utils.streams.ResettableFileInputStream;
import it.unitn.disi.utils.tabular.TableReader;

public class IndexedReader {

	public static IndexedReader createReader(File indexFile, File database)
			throws IOException {
		return new IndexedReader(indexFile, database);
	}

	private IndexEntry[] fIndex;

	private ResettableFileInputStream fStream;

	// -------------------------------------------------------------------------

	private IndexedReader(File index, File database) throws IOException {
		fStream = new ResettableFileInputStream(database);
		fIndex = loadIndex(index);
	}

	// -------------------------------------------------------------------------

	private IndexEntry[] loadIndex(File f) throws IOException {

		System.err.println("-- Index -- ");
		System.err.println("- File is " + f.getName() + ".");
		System.err.print("- Reading...");

		TableReader reader = new TableReader(new FileInputStream(f));
		ArrayList<IndexEntry> entries = new ArrayList<IndexEntry>();
		while (reader.hasNext()) {
			reader.next();
			entries.add(new IndexEntry(Integer.parseInt(reader.get("id")), Long
					.parseLong(reader.get("offset")), Integer.parseInt(reader
					.get("row"))));
		}

		System.err.println("done. ");
		System.err.print("- Processing/sorting...");

		IndexEntry[] index = entries.toArray(new IndexEntry[entries.size()]);
		Arrays.sort(index);

		System.err.println("done. ");

		return index;
	}

	// -------------------------------------------------------------------------

	public IndexEntry select(int id) throws IOException {
		int idx = Arrays.binarySearch(fIndex, id);
		if (idx < 0 || idx >= fIndex.length || fIndex[idx].id != id) {
			return null;
		}

		// Reads assignment.
		long offset = fIndex[idx].offset;
		fStream.reposition(offset);

		return fIndex[idx];
	}
	
	public ResettableFileInputStream getStream() {
		return fStream;
	}

	// -------------------------------------------------------------------------

	static class IndexEntry implements Comparable<Object> {

		public final int id;
		public final int rowStart;
		public final long offset;

		public IndexEntry(int root, long offset, int rowStart) {
			this.id = root;
			this.rowStart = rowStart;
			this.offset = offset;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof Integer) {
				return compareInteger((Integer) o);
			} else {
				return compareEntry((IndexEntry) o);
			}
		}

		private int compareEntry(IndexEntry o) {
			return this.id - o.id;
		}

		private int compareInteger(Integer o) {
			return this.id - o;
		}
	}
}
