package it.unitn.disi.churn.intersync;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import peersim.config.Configuration;
import peersim.util.IncrementalStats;

import it.unitn.disi.churn.BaseChurnSim;
import it.unitn.disi.churn.IChurnSim;
import it.unitn.disi.churn.IValueObserver;
import it.unitn.disi.churn.IncrementalStatsAdapter;
import it.unitn.disi.churn.RenewalProcess;
import it.unitn.disi.churn.RenewalProcess.State;
import it.unitn.disi.network.churn.yao.AveragesFromFile;
import it.unitn.disi.network.churn.yao.YaoInit.IAverageGenerator;
import it.unitn.disi.network.churn.yao.YaoInit.IDistributionGenerator;
import it.unitn.disi.network.churn.yao.YaoPresets;

public class SyncEstimatorCLI {

	static enum ExperimentType {
		true_average, regular, all;
	}

	@Option(name = "-m", aliases = { "--mode" }, usage = "One of the Yao modes.", required = true)
	private String fMode;

	@Option(name = "-r", aliases = { "--repetitions" }, usage = "How many times to repeat the synchronization state.", required = true)
	private int fRepeats;

	@Option(name = "-c", aliases = { "--chainsize" }, usage = "Size of the chain to simulate.", required = true)
	private int fSize;

	@Option(name = "-e", aliases = { "--experiment" }, usage = "One of (regular | true_average).", required = true)
	private String fType;

	@Option(name = "-o", aliases = { "--outer" }, usage = "Uses outer instead of inner repetitions.", required = false)
	private boolean fUseOuter = false;

	@Option(name = "-p", aliases = { "--parfile" }, usage = "File containing li/di parameters.", required = false)
	private String fParfile;

	@Option(name = "-b", aliases = { "--burnin" }, usage = "Simulation burn-in time.", required = false)
	private double fBurnin;

	@Option(name = "-v", aliases = { "--verbose" }, usage = "Verbose session lengths.", required = false)
	private boolean fVerbose = false;

	@Option(name = "-h", aliases = { "--help" }, usage = "prints this help message", required = false)
	private boolean fHelpOnly;

	public void _main(String[] args) throws Exception {
		CmdLineParser parser = new CmdLineParser(this);
		try {
			parser.parseArgument(args);
			ExperimentType.valueOf(fType);
			if (fHelpOnly) {
				printHelp(parser);
				return;
			}
		} catch (CmdLineException ex) {
			System.err.println(ex.getMessage());
			printHelp(parser);
			return;
		} catch (IllegalArgumentException ex) {
			System.err.println("Invalid mode " + fType + ".");
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

		for (int i = 0; i < fSize; i++) {
			li[0] = li[1];
			di[0] = di[1];
			li[1] = agen.nextLI();
			di[1] = agen.nextDI();
			pid[0] = i;
			pid[1] = i + 1;
			runExperiment(agen, dgen, li, di, pid, i);
		}

	}

	// ------------------------------------------------------------------------

	private void runExperiment(IAverageGenerator agen,
			IDistributionGenerator dgen, double[] li, double[] di, int[] pid,
			int id) {

		List<IValueObserver> stats = mkStats();

		// With outer repeats, we start one experiment from scratch each time.
		BaseChurnSim sim = null;
		if (fUseOuter) {
			for (int i = 0; i < fRepeats; i++) {
				sim = experiment(create(pid[0], dgen, li[0], di[0], i != 0),
						create(pid[1], dgen, li[1], di[1], i != 0), fBurnin,
						Integer.toString(i), 1, stats, fVerbose);
				sim.run();
			}
		}

		else {
			System.err.println("Inner repeats");
			sim = experiment(create(pid[0], dgen, li[0], di[0], false),
					create(pid[1], dgen, li[1], di[1], false), fBurnin,
					Integer.toString(id), fRepeats, stats, fVerbose);
			sim.run();
		}

		for (IValueObserver stat : stats) {
			stat.print(System.out);
		}
	}

	private List<IValueObserver> mkStats() {
		List<IValueObserver> stats = new ArrayList<IValueObserver>();
		stats.add(new IncrementalStatsAdapter(new IncrementalStats()));
		if (ExperimentType.valueOf(fType) == ExperimentType.all) {
			stats.add(new IncrementalStatsAdapter(new IncrementalStats()));
		}
		return stats;
	}

	// ------------------------------------------------------------------------

	private BaseChurnSim experiment(RenewalProcess p1, RenewalProcess p2,
			double burnin, String string, int repeats,
			List<IValueObserver> stats, boolean verbose) {

		List<IChurnSim> sims = new ArrayList<IChurnSim>();

		ExperimentType type = ExperimentType.valueOf(fType);
		if (type == ExperimentType.true_average || type == ExperimentType.all) {
			sims.add(new TrueSyncEstimator(repeats, false, stats.remove(0)));
		}

		if (type == ExperimentType.regular || type == ExperimentType.all) {
			sims.add(new BurninSyncEstimator(burnin, repeats, stats.remove(0)));
		}

		return new BaseChurnSim(new RenewalProcess[] { p1, p2 }, sims, 0.0);
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
		System.err.println(this.getClass().getSimpleName() + " [options...]");
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

	public static void main(String[] args) throws Exception {
		new SyncEstimatorCLI()._main(args);
	}

}
