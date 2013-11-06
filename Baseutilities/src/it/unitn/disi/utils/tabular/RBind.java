package it.unitn.disi.utils.tabular;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.exception.ParseException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Binds several tables by row. The header of the first table becomes the
 * "defining header". All subsequent tables being bound must have either the
 * same columns, or a superset of them.
 * 
 * @author giuliano
 */
@AutoConfig
public class RBind implements ITransformer {

	@Attribute(value = "indulging", defaultValue = "false")
	private boolean fIndulging;

	private int fCurrentTable = 1;

	private int fCurrentLine = 1;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader reader = new TableReader(new BufferedInputStream(is));
		String[] header = reader.columns().toArray(new String[] {});
		ITableWriter writer = new TableWriter(new PrintStream(
				new BufferedOutputStream(oup)), header);

		while (reader.hasNext()) {
			try {
				fCurrentLine++;
				reader.next();
			} catch (ParseException ex) {
				reader = nextTable(reader, header);
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

	private TableReader nextTable(TableReader old, String[] header) {
		TableReader newReader = old.fromCurrentRow();
		if (checkHeader(newReader, header, fCurrentLine)) {
			fCurrentTable++;
			return newReader;
		}
		return old;
	}

	private boolean pMatches(String[] header, TableReader reader) {
		for (String key : header) {
			if (!reader.get(key).equals(key)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkHeader(TableReader reader, String[] header, int count) {
		List<String> newHeader = reader.columns();
		for (String headerPart : header) {
			if (!newHeader.contains(headerPart)) {
				if (fIndulging) {
					System.err.println("-- Skipping bad line " + count + ".");
					return false;
				}

				throw new ParseException("Cannot bind by column: table "
						+ fCurrentTable + " does not contain column "
						+ headerPart + " (line " + count + ").");
			}
		}

		return true;
	}

	private void transfer(TableReader reader, ITableWriter writer,
			String[] header) {
		for (String key : header) {
			writer.set(key, reader.get(key));
		}
	}

}
