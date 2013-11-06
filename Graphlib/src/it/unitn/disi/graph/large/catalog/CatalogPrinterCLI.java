package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.cli.ITransformer;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.AutoConfig;

/**
 * CLI command for printing the contents of a catalog in text form.
 * 
 * @author giuliano
 */
@AutoConfig
public class CatalogPrinterCLI implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		// Record types can be made configurable when we have more than one
		// type.
		ICatalogRecordType type = CatalogRecordTypes.PROPERTY_RECORD;

		PrintStream out = new PrintStream(oup);
		CatalogReader cs = new CatalogReader(is, type);

		printHeader(type, out);

		while (cs.hasNext()) {
			cs.next();
			printRow(type, cs, out);
		}
	}

	private void printRow(ICatalogRecordType type, CatalogReader cs,
			PrintStream oup) {
		StringBuffer line = new StringBuffer();
		for (ICatalogPart<? extends Number> part : type.getParts()) {
			line.append(cs.get(part.key()));
			line.append(" ");
		}
		oup.println(line.substring(0, Math.max(0, line.length() - 1)));
	}

	private void printHeader(ICatalogRecordType type, PrintStream oup) {
		StringBuffer header = new StringBuffer();
		for (ICatalogPart<? extends Number> part : type.getParts()) {
			header.append(part.key());
			header.append(" ");
		}
		oup.println(header.substring(0, Math.max(0, header.length() - 1)));
	}

}
