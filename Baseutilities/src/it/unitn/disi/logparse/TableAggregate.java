package it.unitn.disi.logparse;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.utils.tabular.TableReader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

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

	public TableAggregate(
			@Attribute(value = "file_list", description = "Space-separated list of files to process.") String fileList,
			@Attribute(value = "aggregate", description = "Space-separated list of fields to be aggregated.") String aggregate,
			@Attribute(value = "ignore", description = "Space-separated list of fields to be ignored (won't be printed in the output).") String ignores,
			@Attribute(value = "aggregator", description = "Aggregation operation to apply.") String aggregator,
			@Attribute(value = "aggregation_count", description = "Field where counts of aggregation are stored.") String countField) {

		fFileList = fileList.split(SEPARATOR_CHAR);
		fIgnores = ignores.split(SEPARATOR_CHAR);

		String[] aggregatables = aggregate.split(SEPARATOR_CHAR);
		FieldDispatcher dispatcher = new FieldDispatcher();
		dispatcher.register(new SumAggregation(1.0), countField);
		dispatcher.register(chooseOp(aggregator), aggregatables);
		fOp = dispatcher;
		
		fAggregatables = new String[aggregatables.length + 1];
		System.arraycopy(aggregatables, 0, fAggregatables, 0,
				aggregatables.length);
		fAggregatables[fAggregatables.length - 1] = countField;
	}

	@Override
	public void execute(StreamProvider provider) throws Exception {
		PrintStream out = new PrintStream(new BufferedOutputStream(
				provider.output(Outputs.aggregate)));
		TableReader[] fTables = openTables(fFileList);

		// Picks one table as leading, arbitrarily.
		TableReader leading = fTables[0];

		// Computes leads.
		String[] leads = computeLeads(leading);
		
		// Prints the header.
		printHeader(out, leads);

		int lineNo = 0;
		while (leading.hasNext()) {
			lineNo++;
			// Iterates all tables.
			for (TableReader table : fTables) {
				table.next();
			}
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
		}
		out.flush();
		System.err.println("Lead table: " + lineNo + " lines.");
	}

	private void printHeader(PrintStream out, String [] leads) {
		StringBuffer header = new StringBuffer();
		
		for (String key : leads) {
			header.append(key);
			header.append(FIELD_SEPARATOR);
		}
		
		for (String key : fAggregatables) {
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

	/**
	 * Enables different {@link IAggregationOp} to be used for each field.
	 * 
	 * @author giuliano
	 */
	static class FieldDispatcher implements IAggregationOp {

		private final Map<String, IAggregationOp> fDispatchTable;

		public FieldDispatcher() {
			fDispatchTable = new HashMap<String, IAggregationOp>();
		}

		public void register(IAggregationOp op, String... fields) {
			for (String field : fields) {
				fDispatchTable.put(field, op);
			}
		}

		@Override
		public String aggregate(TableReader[] readers, String fieldKey) {
			return fDispatchTable.get(fieldKey).aggregate(readers, fieldKey);
		}

	}

	static class SumAggregation implements IAggregationOp {

		private Double fDefault;

		public SumAggregation() {
			this(null);
		}

		public SumAggregation(Double dephault) {
			fDefault = dephault;
		}

		@Override
		public String aggregate(TableReader[] readers, String fieldKey) {
			double aggregate = 0.0;
			for (TableReader reader : readers) {
				String value = reader.get(fieldKey);
				double doubleValue;
				// XXX resolving default values doesn't really belong here,
				// but to a wrapping layer over TableReader.
				if (value == null) {
					if (fDefault == null) {
						throw new IllegalArgumentException("Unknown field " + fieldKey + ".");
					}
					doubleValue = fDefault;
				} else {
					doubleValue = Double.parseDouble(value);
				}
				aggregate += doubleValue;
			}
			return Double.toString(aggregate);
		}

	}
}
