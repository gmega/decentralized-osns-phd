package it.unitn.disi.churn.connectivity.p2p;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableReader.ILineReader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class ExperimentIndexer implements ITransformer {

	@Attribute("assignments")
	private String fAssignments;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		final RandomAccessFile raf = new RandomAccessFile(
				new File(fAssignments), "r");
		
		TrackingReader tracker = new TrackingReader(raf);
		TableReader reader = new TableReader(tracker);
		TableWriter writer = new TableWriter(oup, "id", "offset", "row");
		
		String root = "";
		int row = 1;
		while(reader.hasNext()) {
			reader.next();
			row++;
			String cRoot = reader.get("id");
			if (!root.equals(cRoot)) {
				writer.set("id", cRoot);
				writer.set("offset", tracker.currentLineOffset);
				writer.set("row", row);
				writer.emmitRow();
				root = cRoot;
			}	
		}
	}

	static class TrackingReader implements ILineReader {
		
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
			// TODO Auto-generated method stub
			
		}
	}
}
