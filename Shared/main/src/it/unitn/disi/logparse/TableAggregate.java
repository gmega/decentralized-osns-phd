package it.unitn.disi.logparse;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.utils.TableReader;

/**
 * Highly scalable aggregation script for tables -- merges 'n' tables into one
 * by using a customizable aggregation routine.
 * 
 * @author giuliano
 */
@AutoConfig
public class TableAggregate implements IMultiTransformer {

	public static enum Outputs {
		aggregate
	}

	private static final String SEPARATOR_CHAR = " ";

	private static final String FIELD_SEPARATOR = " ";

	private final String[] fFileList;

	private final String[] fAggregatables;

	private final String[] fIgnores;

	private final IAggregationOp fOp;

	public TableAggregate(@Attribute("file_list") String fileList,
			@Attribute("aggregate") String aggregate,
			@Attribute("ignore") String ignores,
			@Attribute("aggregator") String aggregator) {
		fFileList = fileList.split(SEPARATOR_CHAR);
		fAggregatables = aggregate.split(SEPARATOR_CHAR);
		fIgnores = ignores.split(SEPARATOR_CHAR);
		fOp = chooseOp(aggregator);
	}

	@Override
	public void execute(StreamProvider provider) throws Exception {
		PrintStream out = new PrintStream(new BufferedOutputStream(
				provider.output(Outputs.aggregate)));
		TableReader[] fTables = openTables(fFileList);

		// Picks one table as leading, arbitrarily.
		TableReader leading = fTables[0];

		// Prints the header.
		printHeader(out, leading);

		// Computes leads.
		String[] leads = computeLeads(leading);

		int lineNo = 0;
		while (leading.hasNext()) {
			lineNo++;
			// Checks that the leading fields coincide.
			StringBuffer line = new StringBuffer();
			checkLeads(fTables, leads);

			// Appends leads.
			for (String lead : leads) {
				line.append(leading.get(lead));
				line.append(FIELD_SEPARATOR);
			}

			// Aggregates fields.
			for (String key : fAggregatables) {
				line.append(fOp.aggregate(fTables, key));
				line.append(FIELD_SEPARATOR);
			}
			out.println(line.toString());

			// Iterates all tables.
			for (TableReader table : fTables) {
				table.next();
			}
		}
		out.flush();
		System.err.println("Lead table: " + lineNo + " lines.");
	}

	private void printHeader(PrintStream out, TableReader leading) {
		StringBuffer header = new StringBuffer();
		for (String key : leading.columns()) {
			header.append(key);
			header.append(FIELD_SEPARATOR);
		}
		out.println(header.toString());
	}

	private String[] computeLeads(TableReader lead) {
		ArrayList<String> leads = new ArrayList<String>();
		for (String column : lead.columns()) {
			if (!in(column, fAggregatables) && !in(column, fIgnores)) {
				leads.add(column);
			}
		}
		return leads.toArray(new String[leads.size()]);
	}

	private boolean in(String field, String[] array) {
		for (String aggregatable : array) {
			if (aggregatable.equals(field)) {
				return true;
			}
		}
		return false;
	}

	private void checkLeads(TableReader[] fTables, String[] leads) {
		for (String lead : leads) {
			String reference = fTables[0].get(lead);
			for (int j = 1; j < fTables.length; j++) {
				if (!reference.equals(fTables[j].get(lead))) {
					throw new IllegalStateException("Lead check failure: "
							+ lead + ".");
				}
			}
		}
	}

	private TableReader[] openTables(String[] files) throws IOException {
		TableReader[] readers = new TableReader[files.length];
		for (int i = 0; i < files.length; i++) {
			readers[i] = new TableReader(
					new FileInputStream(new File(files[i])));
		}
		return readers;
	}

	private IAggregationOp chooseOp(String aggregate) {
		if (aggregate.equals("sum")) {
			return new SumAggregation();
		} else {
			throw new IllegalArgumentException("Unknown aggreagator "
					+ aggregate + ".");
		}
	}

	static interface IAggregationOp {
		String aggregate(TableReader[] readers, String fieldKey);
	}

	static class SumAggregation implements IAggregationOp {
		@Override
		public String aggregate(TableReader[] readers, String fieldKey) {
			double aggregate = 0.0;
			for (TableReader reader : readers) {
				aggregate += Double.parseDouble(reader.get(fieldKey));
			}
			return Double.toString(aggregate);
		}

	}
}
