package it.unitn.disi.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Utility class which allows tabular data to be written in a simple way.
 * 
 * @author giuliano
 */
public class TableWriter {

	private static final String FS = " ";

	private final String[] fFields;

	private String[] fCurrentRecord;

	private boolean fPrintHeader;

	private final PrintWriter fOut;

	private final String fSeparator;

	public TableWriter(PrintStream output, String... fields) {
		this(output, FS, fields);
	}
	
	public TableWriter(PrintWriter output, String... fields) {
		this(output, FS, fields);
	}
	
	public TableWriter(PrintStream output, String separator, String... fields) {
		this(new PrintWriter(output), separator, fields);
	}

	public TableWriter(PrintWriter output, String separator, String... fields) {
		fOut = output;
		fFields = fields;
		fSeparator = separator;
		fCurrentRecord = new String[fFields.length];
	}

	public void newRow() {
		fOut.flush();
		Arrays.fill(fCurrentRecord, null);
	}
	
	public boolean set(String key, String value) {
		int idx = indexOf(key);
		if (idx == -1) {
			return false;
		}
		fCurrentRecord[idx] = value;
		return true;
	}
	
	public boolean set(String key, int value) {
		return this.set(key, Integer.toString(value));
	}
	
	public boolean set(String key, long value) {
		return this.set(key, Long.toString(value));
	}
	
	public boolean set(String key, double value) {
		return this.set(key, Double.toString(value));
	}

	public boolean set(String key, float value) {
		return this.set(key, Float.toString(value));
	}
	
	public boolean set(String key, Object object) {
		return this.set(key, object.toString());
	}

	public void emmitRow() {
		this.emmitHeader();
		StringBuffer rb = new StringBuffer();
		for (int i = 0; i < fCurrentRecord.length; i++) {
			if (fCurrentRecord[i] == null) {
				throw new IllegalStateException("Record field " + fFields[i]
						+ " not set.");
			}
			rb.append(fCurrentRecord[i]);
			rb.append(fSeparator);
		}
		trim(rb);
		fOut.println(rb.toString());
		this.newRow();
	}

	private void emmitHeader() {
		if (fPrintHeader) {
			return;
		}
		fPrintHeader = true;
		StringBuffer hb = new StringBuffer();
		for (String field : fFields) {
			hb.append(field);
			hb.append(fSeparator);
		}
		trim(hb);
		fOut.println(hb.toString());
	}

	private void trim(StringBuffer hb) {
		hb.delete(hb.length() - fSeparator.length(), hb.length());
	}

	private int indexOf(String key) {
		int idx = -1;
		for (int i = 0; i < fFields.length; i++) {
			if (fFields[i].equals(key)) {
				idx = i;
				break;
			}
		}
		return idx;
	}
}
