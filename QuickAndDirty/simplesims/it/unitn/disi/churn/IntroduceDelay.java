package it.unitn.disi.churn;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;

@AutoConfig
public class IntroduceDelay implements ITransformer {

	@Attribute
	double delay;

	@Attribute
	boolean output;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader reader = new TableReader(is);
		TableWriter writer = new TableWriter(new PrintStream(oup), "started",
				"finished", "duration");

		IncrementalStats durations = new IncrementalStats();
		IncrementalStats unbiased = new IncrementalStats();
		IncrementalStats skipped = new IncrementalStats();

		double end = 0;
		double nextStart = 0;

		int line = 0, exps = 0, total = 0;

		try {
			while (reader.hasNext()) {
				reader.next();
				line++;

				unbiased.add(Double.parseDouble(reader.get("duration")));
				total++;

				double start = Double.parseDouble(reader.get("started"));
				if (start < nextStart) {
					skipped.add(Double.parseDouble(reader.get("duration")));
					continue;
				}

				exps++;
				end = Double.parseDouble(reader.get("finished"));
				durations.add(end - start);

				if (output) {
					writer.set("started", start);
					writer.set("finished", end);
					writer.set("duration", (start - end));
					writer.emmitRow();
				}

				nextStart = end + delay;
			}
		} catch (NumberFormatException ex) {
			System.err.println("Failed to parse number at line " + line + ".");
		}

		System.out.println((durations.getAverage() * 3600) + " "
				+ (unbiased.getAverage() * 3600) + " "
				+ (skipped.getAverage() * 3600) + " " + exps + " " + total
				+ " " + ((double) exps) / ((double) total));
	}
}
