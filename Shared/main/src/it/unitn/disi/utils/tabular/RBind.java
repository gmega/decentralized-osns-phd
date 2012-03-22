package it.unitn.disi.utils.tabular;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.exception.ParseException;

/**
 * Binds several tables by row. The header of the first table becomes the
 * "defining header". All subsequent tables being bound must have either the
 * same columns, or a superset of them.
 *  
 * @author giuliano
 */
@AutoConfig
public class RBind implements ITransformer {
	
	private int fCurrentTable = 1;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader reader = new TableReader(new BufferedInputStream(is));
		String[] header = reader.columns().toArray(new String[] {});
		ITableWriter writer = new TableWriter(new PrintStream(
				new BufferedOutputStream(oup)), header);

		int lineCount = 1;
		while (reader.hasNext()) {
			try {
				lineCount++;
				reader.next();
			} catch (ParseException ex) {
				reader = reader.fromCurrentRow();
				fCurrentTable++;
				checkHeader(reader, header, lineCount);
				continue;
			}
			
			// Is the current row a header?
			if (pMatches(header, reader)) {
				// Yes, don't transfer.
				continue;
			}

			transfer(reader, writer, header);
			writer.emmitRow();
		}
	}
	
	private boolean pMatches(String[] header, TableReader reader) {
		for (String key : header) {
			if (!reader.get(key).equals(key)) {
				return false;
			}
		}
		return true;
	}

	private void checkHeader(TableReader reader, String[] header, int count) {
		List <String> newHeader = reader.columns();
		for (String headerPart : header) {
			if (!newHeader.contains(headerPart)) {
				throw new ParseException("Cannot bind by column: table "
						+ fCurrentTable + " does not contain column "
						+ headerPart + " (line " + count + ").");
			}
		}
	}

	private void transfer(TableReader reader, ITableWriter writer,
			String[] header) {
		for (String key : header) {
			writer.set(key, reader.get(key));
		}
	}

}
