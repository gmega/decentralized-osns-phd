package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.LightweightStaticGraph;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.collections.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;
import peersim.util.IncrementalStats;

/**
 * {@link LoadSimulator} is a "what if" simulator able to replay load data
 * belonging to previously ran unit experiments, and compute what the bandwidth
 * requirements under those circumstances would be.
 * 
 * @author giuliano
 */
@AutoConfig
public class LoadSimulator implements IMultiTransformer, ILoadSim {

	/**
	 * {@link StreamProvider} input keys.
	 */
	public static enum Inputs {
		graph, experiments;
	}

	/**
	 * {@link StreamProvider} output keys.
	 */
	public static enum Outputs {
		load;
	}

	public static enum PrintMode {
		all, root
	}

	/**
	 * Unit experiment row keys for {@link TableReader}.
	 */
	private static final String EXPERIMENT_ID = "root_id";
	private static final String NODE_ID = "neighbor_id";
	private static final String SENT = "sent";
	private static final String RECEIVED = "received";
	private static final String DUPLICATES = "duplicates";

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	/**
	 * The {@link IndexedNeighborGraph} containing the neighbor dependencies
	 * between the unit experiments.
	 */
	private volatile IndexedNeighborGraph fGraph;

	private String fDecoder;

	private final PrintMode fPrintMode;

	/**
	 * The percentage of the neighborhood to keep active during the what-if
	 * analysis.
	 */
	private final double fPercentage;

	/**
	 * {@link Executor} used to run the experiments.
	 */
	private final ThreadPoolExecutor fExecutor;

	/**
	 * Unit experiment data. Currently, it is pre-loaded into memory.
	 */
	private volatile Map<Integer, UnitExperiment> fExperiments;

	/**
	 * {@link Semaphore} for governing access to the cores.
	 */
	private final Semaphore fSema;

	/**
	 * Random number generator for the {@link PercentageRandomScheduler}.
	 */
	private final Random fRandom;

	/**
	 * Output {@link PrintStream}.
	 */
	private PrintStream fStream;

	// ----------------------------------------------------------------------

	public LoadSimulator(
			@Attribute(value = "print_mode", defaultValue = "root") String printMode,
			@Attribute("percentage") double percentage,
			@Attribute("cores") int cores,
			@Attribute(value = "decoder", defaultValue = Attribute.VALUE_NULL) String decoder) {

		fExecutor = new CallbackThreadPoolExecutor(cores, this);
		fSema = new Semaphore(cores);
		fPercentage = percentage;
		fRandom = new Random(42);
		fPrintMode = PrintMode.valueOf(printMode.toLowerCase());
		fDecoder = decoder;
	}

	// ----------------------------------------------------------------------
	// IMultiTransformer interface.
	// ----------------------------------------------------------------------

	@Override
	public void execute(StreamProvider p) throws IOException {
		// Loads the graph.
		fGraph = LightweightStaticGraph.load(decoder(p.input(Inputs.graph)));

		// Loads the unit experiments.
		fExperiments = loadUnitExperiments(fGraph, p.input(Inputs.experiments));

		// Sets up the output stream.
		fStream = new PrintStream(p.output(Outputs.load));

		// Runs the experiments
		try {
			runExperiments();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private void runExperiments() throws InterruptedException {
		System.err.println("Now running simulations using ["
				+ fExecutor.getCorePoolSize() + "] threads.");

		Iterator<UnitExperiment> it = fExperiments.values().iterator();
		while (it.hasNext()) {
			acquireCore();
			if (fExecutor.isTerminating()) {
				break;
			}
			UnitExperiment experiment = it.next();
			PercentageRandomScheduler scheduler = new PercentageRandomScheduler(
					this, experiment.id(), fPercentage, fRandom);
			ExperimentRunner runner = new ExperimentRunner(experiment,
					scheduler, this);
			fExecutor.submit(runner);
		}

		System.err.print("Done. Shut down... ");
		fExecutor.shutdown();
		System.err.println("[OK]");
	}

	// ----------------------------------------------------------------------

	private ResettableGraphDecoder decoder(InputStream stream) {
		String decoder = (fDecoder == null) ? AdjListGraphDecoder.class
				.getName() : fDecoder;
		try {
			return GraphCodecHelper.createDecoder(stream, decoder);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------

	private synchronized void appendStatistic(IncrementalStats stats,
			StringBuffer buffer) {
		buffer.append(stats.getMin());
		buffer.append(" ");
		buffer.append(stats.getMax());
		buffer.append(" ");
		buffer.append(stats.getAverage());
		buffer.append(" ");
		buffer.append(stats.getVar());
	}

	// ----------------------------------------------------------------------

	private Map<Integer, UnitExperiment> loadUnitExperiments(Graph graph,
			InputStream input) throws IOException {

		System.err.print("Reading unit experiment data ... ");

		Map<Integer, UnitExperiment> experiments = new HashMap<Integer, UnitExperiment>();

		// Now loads the actual data.
		TableReader reader = new TableReader(input);
		while (reader.hasNext()) {
			int id = Integer.parseInt(reader.get(EXPERIMENT_ID));
			int nodeId = Integer.parseInt(reader.get(NODE_ID));
			int sent = Integer.parseInt(reader.get(SENT));
			int received = Integer.parseInt(reader.get(RECEIVED));
			int dups = Integer.parseInt(reader.get(DUPLICATES));

			UnitExperiment experiment = experiments.get(id);
			if (experiment == null) {
				experiment = new UnitExperiment(id, graph.degree(id), true);
				experiments.put(id, experiment);
			}

			experiment.addData(nodeId, sent, received + dups);
			reader.next();
		}

		// Wraps up the experiments.
		for (UnitExperiment experiment : experiments.values()) {
			experiment.done();
		}

		System.err.println("[OK]");
		return experiments;
	}

	// ----------------------------------------------------------------------
	// Callbacks.
	// ----------------------------------------------------------------------

	public void printSummary(int root,
			Collection<? extends MessageStatistics> collection) {
		for (MessageStatistics statistic : collection) {
			if (!shouldPrintData(root, statistic.id)) {
				continue;
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(root);
			buffer.append(" ");
			buffer.append(statistic.id);
			buffer.append(" ");
			buffer.append(statistic.sent);
			buffer.append(" ");
			buffer.append(statistic.received);
			buffer.append(" ");
			appendStatistic(statistic.sendBandwidth, buffer);
			buffer.append(" ");
			appendStatistic(statistic.receiveBandwidth, buffer);

			synchronizedPrint(buffer.toString());
		}
	}
	
	// ----------------------------------------------------------------------

	public void acquireCore() throws InterruptedException {
		fSema.acquire();
	}

	// ----------------------------------------------------------------------

	public void releaseCore() {
		fSema.release();
	}

	// ----------------------------------------------------------------------
	// ILoadSim interface.
	// ----------------------------------------------------------------------
	
	public boolean shouldPrintData(int root, int node) {
		switch(fPrintMode) {
		case all:
			return true;
		case root:
			return root == node;
		}
		
		// Shouldn't get here.
		throw new IllegalStateException("Internal error.");
	}

	// ----------------------------------------------------------------------
	
	@Override
	public synchronized void synchronizedPrint(String data) {
		fStream.println(data.toString());
	}

	// ----------------------------------------------------------------------

	@Override
	public UnitExperiment unitExperiment(int index) {
		if (!fExperiments.containsKey(index)) {
			fExecutor.shutdown();
			throw new NoSuchElementException(
					"ERROR: Missing unit experiment data " + index + ".");
		}

		return fExperiments.get(index);
	}

	// ----------------------------------------------------------------------

	@Override
	public IndexedNeighborGraph getGraph() {
		return fGraph;
	}
}

/**
 * {@link ThreadPoolExecutor} subclass which calls back the {@link ILoadSim}
 * when an experiment completes.
 * 
 * @author giuliano
 */
class CallbackThreadPoolExecutor extends ThreadPoolExecutor {

	private final LoadSimulator fLoadSim;

	public CallbackThreadPoolExecutor(int cores, LoadSimulator loadSim) {
		super(cores, cores, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		fLoadSim = loadSim;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void afterExecute(Runnable r, Throwable t) {
		Future<Pair<Integer, Collection<? extends MessageStatistics>>> future = (Future<Pair<Integer, Collection<? extends MessageStatistics>>>) r;

		Pair<Integer, Collection<? extends MessageStatistics>> statistics = null;

		try {
			statistics = future.get();
		} catch (ExecutionException ex) {
			System.err.println("Error while running task.");
			ex.printStackTrace();
			this.shutdown();
			return;
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} finally {
			fLoadSim.releaseCore();
		}

		fLoadSim.printSummary(statistics.a, statistics.b);
	}
}
