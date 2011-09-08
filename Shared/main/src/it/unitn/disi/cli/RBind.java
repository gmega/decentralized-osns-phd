package it.unitn.disi.cli;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.AutoConfig;

import it.unitn.disi.utils.exception.ParseException;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * Binds several tables by row. The header of the first table becomes the
 * "defining header". All subsequent tables being bound must have either the
 * same columns, or a superset of them.
 *  
 * @author giuliano
 */
@AutoConfig
public class RBind implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader reader = new TableReader(new BufferedInputStream(is));
		String[] header = reader.columns().toArray(new String[] {});
		ITableWriter writer = new TableWriter(new PrintStream(
				new BufferedOutputStream(oup)), header);

		while (reader.hasNext()) {
			try {
				reader.next();
			} catch (ParseException ex) {
				// As long as the new width keeps the same fields, should be ok.
				reader = reader.fromCurrentRow();
				continue;
			}

			if (pMatches(header, reader)) {
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

	private void transfer(TableReader reader, ITableWriter writer,
			String[] header) {
		for (String key : header) {
			writer.set(key, reader.get(key));
		}
	}

}
