package it.unitn.disi.statistics;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.SparseMultiCounter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class ECDF implements ITransformer {

	@Attribute("column")
	String fColumn;
	
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		SparseMultiCounter<String> counter = new SparseMultiCounter<String>();
		TableReader reader = new TableReader(is);

		System.err.print("Computing knots and frequencies...");
		while (reader.hasNext()) {
			reader.next();
			counter.increment(reader.get(fColumn));
		}
		System.err.println(counter.asMap().size() + " knots found.");
		
		System.err.println("Sorting and computing cumulative frequencies.");
		// Computes the stepping points.
		Map<String, Integer> counts = counter.asMap();
		long [] knots = new long[counts.size()];
		Iterator<String> it = counts.keySet().iterator();
		for (int i = 0; i < knots.length; i++) {
			knots[i] = Long.parseLong(it.next());
		}
		Arrays.sort(knots);
		
		// Computes the cumulative frequencies and total number of elements.
		double total = 0;
		long [] freqs = new long[knots.length];
		for (int i = 0; i < knots.length; i++) {
			int count = counts.get(Long.toString(knots[i]));
			freqs[i] = count;
			// Accumulates the frequency.
			if (i > 0) {
				freqs[i] += freqs[i - 1];
			}
			total += count;
		}
		
		System.err.println("Outputting results.");
		TableWriter writer = new TableWriter(new PrintStream(oup), "value",
				"count", "cdf");
		for (int i = 0; i < knots.length; i++) {
			long freq = (i > 0) ? (freqs[i] - freqs[i - 1]) : freqs[i];
			double cdf = freqs[i]/total;
			
			writer.set("value", knots[i]);
			writer.set("count", freq);
			writer.set("cdf", cdf);
			writer.emmitRow();
		}
	}

}
