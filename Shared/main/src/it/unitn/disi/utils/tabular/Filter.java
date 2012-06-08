package it.unitn.disi.utils.tabular;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.collections.Triplet;

@AutoConfig
public class Filter implements ITransformer {

	@Attribute("source")
	private String fKeySource;

	@Attribute("keyfields")
	private String fKeyFields;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		String[] keys = fKeyFields.split(",");
		if (keys.length > 3) {
			throw new UnsupportedOperationException(
					"Only 3 fields are supported.");
		}

		if (keys.length == 2) {
			keys = new String[] { keys[0], keys[1], null };
		}

		PrintStream out = new PrintStream(oup);

		HashSet<Triplet<String, String, String>> allowed = new HashSet<Triplet<String, String, String>>();
		TableReader keyReader = new TableReader(new FileInputStream(new File(
				fKeySource)));

		System.err.print("Read keys...");
		while (keyReader.hasNext()) {
			keyReader.next();
			allowed.add(new Triplet<String, String, String>(keyReader
					.get(keys[0]), keyReader.get(keys[1]), keyReader
					.get(keys[2])));
		}
		System.err.println("[done]");

		int processed = 0;
		int filtered = 0;
		System.err.print("Process input...");
		TableReader table = new TableReader(is);
		while (table.hasNext()) {
			processed++;
			table.next();
			if (allowed.contains(new Triplet<String, String, String>(table
					.get(keys[0]), table.get(keys[1]), table.get(keys[2])))) {
				out.println(table.currentLine());
			} else {
				filtered++;
			}
		}

		System.err.println("[done]");
		System.err.println(processed + " lines processed, " + filtered
				+ " lines filtered.");
	}
}
