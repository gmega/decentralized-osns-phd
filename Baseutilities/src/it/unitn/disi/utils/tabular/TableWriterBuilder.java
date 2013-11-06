package it.unitn.disi.utils.tabular;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Utility class for building {@link TableWriter}s.
 * 
 * @author giuliano
 */
public class TableWriterBuilder {

	private final ArrayList<String> fFields = new ArrayList<String>();

	public TableWriterBuilder() {

	}

	public void addFields(String... fields) {
		for (int i = 0; i < fields.length; i++) {
			addField(fields[i]);
		}
	}

	public boolean addField(String field) {
		boolean shouldAdd = !fFields.contains(field);
		if (shouldAdd) {
			fFields.add(field);
		}
		return shouldAdd;
	}

	public boolean removeField(String field) {
		return fFields.remove(field);
	}

	public TableWriter tableWriter(OutputStream stream) {
		return new TableWriter(stream, fields());
	}

	public TableWriter tableWriter(PrintStream stream) {
		return new TableWriter(stream, fields());
	}

	public TableWriter tableWriter(Writer writer) {
		return new TableWriter(writer, fields());
	}

	private String[] fields() {
		return fFields.toArray(new String[fFields.size()]);
	}
}
