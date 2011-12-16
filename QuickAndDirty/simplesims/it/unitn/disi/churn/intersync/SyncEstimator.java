package it.unitn.disi.churn.intersync;

import java.util.Properties;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import peersim.config.Configuration;
import peersim.util.IncrementalStats;

import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.network.churn.yao.AveragesFromFile;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.network.churn.yao.YaoPresets;

public class SyncEstimator {

	@Option(name = "-m", aliases = { "--mode" }, usage = "One of the Yao modes.", required = true)
	private String fMode;

	@Option(name = "-r", aliases = { "--repetitions" }, usage = "How many times to repeat the synchronization state.", required = true)
	private int fRepeats;

	@Option(name = "-c", aliases = { "--chainsize" }, usage = "Size of the chain to simulate.", required = true)
	private int fSize;

	@Option(name = "-o", aliases = { "--outer" }, usage = "Uses outer instead of inner repetitions.", required = false)
	private boolean fUseOuter = false;

	@Option(name = "-p", aliases = { "--parfile" }, usage = "File containing li/di parameters.", required = false)
	private String fParfile;

	@Option(name = "-b", aliases = { "--burnin" }, usage = "Simulation burn-in time.", required = false)
	private double fBurnin;
	
	@Option(name = "-v", aliases = { "--verbose"}, usage = "Verbose session lengths.", required = false)
	private boolean fVerbose = false;

	@Option(name = "-h", aliases = { "--help" }, usage = "prints this help message", required = false)
	private boolean fHelpOnly;

	public void _main(String[] args) throws Exception {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);
			if (fHelpOnly) {
				printHelp(parser);
				return;
			}
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage());
			printHelp(parser);
			return;
		}

		Configuration.setConfig(new Properties());

		IAverageGenerator agen = averageGenerator();
		IDistributionGenerator dgen = YaoPresets.mode(fMode);

		System.out.println("P:p di li");

		double[] li = new double[2], di = new double[2];
		li[1] = agen.nextLI();
		di[1] = agen.nextDI();

		int[] pid = new int[2];

		for (int i = 1; i <= fSize; i++) {
			li[0] = li[1];
			di[0] = di[1];
			li[1] = agen.nextLI();
			di[1] = agen.nextDI();
			pid[0] = i;
			pid[1] = i + 1;
			IncrementalStats stats = runExperiment(agen, dgen, li, di, pid, i);
			printResults(stats);
		}

	}

	// ------------------------------------------------------------------------

	private IncrementalStats runExperiment(IAverageGenerator agen,
			IDistributionGenerator dgen, double[] li, double[] di, int[] pid,
			int id) {

		IncrementalStats stats = new IncrementalStats();

		// With outer repeats, we start one experiment from scratch each time.
		SyncExperiment exp;
		if (fUseOuter) {
			for (int i = 0; i < fRepeats; i++) {
				exp = new SyncExperiment(create(pid[0], dgen, li[0], di[0],
						i != 0), create(pid[1], dgen, li[1], di[1], i != 0),
						fBurnin, Integer.toString(i), 1, stats, fVerbose);
				exp.run();
			}
		}

		else {
			System.err.println("Inner repeats");
			exp = new SyncExperiment(create(pid[0], dgen, li[0], di[0], false),
					create(pid[1], dgen, li[1], di[1], false), fBurnin,
					Integer.toString(id), fRepeats, stats, fVerbose);
			exp.run();
		}

		return stats;
	}
	
	// ------------------------------------------------------------------------

	private IAverageGenerator averageGenerator() {
		if (fParfile != null) {
			return new AveragesFromFile(fParfile, true);
		}

		return YaoPresets.averageGenerator("yao");
	}

	// ------------------------------------------------------------------------

	private void printHelp(CmdLineParser parser) {
		System.err.println(this.getClass().getSimpleName()
				+ " [options...]");
		parser.printUsage(System.err);
		System.err.println();
	}

	// ------------------------------------------------------------------------

	private static RenewalProcess create(int i, IDistributionGenerator dp,
			double li, double di, boolean quiet) {

		if (!quiet) {
			StringBuffer buf = new StringBuffer();
			buf.append("P:");
			buf.append(i);
			buf.append(" ");
			buf.append(di);
			buf.append(" ");
			buf.append(li);
			System.out.println(buf);
		}

		return new RenewalProcess(i, dp.uptimeDistribution(li),
				dp.downtimeDistribution(di), State.up);
	}

	// ------------------------------------------------------------------------

	private void printResults(IncrementalStats stats) {
		// Done. Prints the statistics for the 11 and 10 states.
		StringBuffer buffer = new StringBuffer();
		buffer.append("E:");

		double lubound = stats.getAverage();
		buffer.append(lubound);
		buffer.append(" ");
		buffer.append(lubound * 3600);
		System.out.println(buffer);

	}
	
	// ------------------------------------------------------------------------

	public static void main(String[] args) throws Exception {
		new SyncEstimator()._main(args);
	}

}
