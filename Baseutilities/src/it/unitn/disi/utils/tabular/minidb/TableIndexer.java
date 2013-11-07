package it.unitn.disi.utils.tabular.minidb;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableReader.ILineReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Builds an row index for a tabular file. The colum that dictates the row index
 * is specified by its header name.
 * 
 * @author giuliano
 */
@AutoConfig
public class TableIndexer implements ITransformer {

	private String fFile;

	private String fIndexColumn;

	public TableIndexer(@Attribute("file") String file,
			@Attribute("index_column") String indexColumn) {
		fFile = file;
		fIndexColumn = indexColumn;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		final RandomAccessFile raf = new RandomAccessFile(new File(fFile), "r");

		TrackingReader tracker = new TrackingReader(raf);
		TableReader reader = new TableReader(tracker);
		TableWriter writer = new TableWriter(oup, fIndexColumn, "offset", "row");

		String root = "";
		int row = 1;
		while (reader.hasNext()) {
			reader.next();
			row++;
			String cRoot = reader.get(fIndexColumn);
			if (!root.equals(cRoot)) {
				writer.set(fIndexColumn, cRoot);
				writer.set("offset", tracker.currentLineOffset);
				writer.set("row", row);
				writer.emmitRow();
				root = cRoot;
			}
		}
	}

	private static class TrackingReader implements ILineReader {

		public long currentLineOffset;

		private long fBufferedLineOffset;

		private RandomAccessFile fFile;

		public TrackingReader(RandomAccessFile file) {
			fFile = file;
		}

		@Override
		public void close() throws IOException {
		}

		@Override
		public String readLine() throws IOException {
			currentLineOffset = fBufferedLineOffset;
			fBufferedLineOffset = fFile.getFilePointer();
			return fFile.readLine();
		}

		@Override
		public void flushBuffers() throws IOException {
		}
	}
}
