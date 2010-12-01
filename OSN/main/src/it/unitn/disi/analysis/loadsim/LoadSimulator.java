package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.HashMapResolver;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

import java.io.IOException;
import java.io.PrintStream;
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
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.config.resolvers.CompositeResolver;
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

	public static enum SimulationMode {
		all, root
	}

	public static enum PrintMode {
		all, summary_only
	}

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	/**
	 * The {@link IndexedNeighborGraph} containing the neighbor dependencies
	 * between the unit experiments.
	 */
	private volatile IndexedNeighborGraph fGraph;

	/**
	 * An {@link IndexedNeighborGraph} decoder.
	 */
	private String fDecoder;

	/**
	 * An {@link IScheduler}.
	 */
	private String fScheduler;

	/**
	 * An {@link IMessageSizeGenerator}.
	 */
	private String fSizeGenerator;

	/**
	 * @see SimulationMode
	 */
	private final SimulationMode fSimMode;

	/**
	 * @see PrintMode
	 */
	private final PrintMode fPrintMode;

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
	 * Random number generator for the {@link ContinuousRandomScheduler}.
	 */
	private final Random fRandom;

	/**
	 * Output {@link PrintStream}.
	 */
	private PrintStream fStream;

	private final IResolver fResolver;

	// ----------------------------------------------------------------------

	public LoadSimulator(
			@Attribute IResolver resolver,
			@Attribute("scheduler") String scheduler,
			@Attribute(value = "print_mode", defaultValue = "summary_only") String printMode,
			@Attribute(value = "sim_mode", defaultValue = "root") String simMode,
			@Attribute(value = "seed", defaultValue = Attribute.VALUE_NONE) String randomSeed,
			@Attribute("cores") int cores,
			@Attribute(value = "decoder", defaultValue = Attribute.VALUE_NULL) String decoder,
			@Attribute(value = "size_generator", defaultValue = Attribute.VALUE_NULL) String sizeGenerator) {

		fSimMode = SimulationMode.valueOf(simMode.toLowerCase());
		fPrintMode = PrintMode.valueOf(printMode.toLowerCase());
		fExecutor = new CallbackThreadPoolExecutor(cores, this);
		fSema = new Semaphore(cores);
		fResolver = resolver;
		fDecoder = decoder;
		fScheduler = scheduler;
		fSizeGenerator = sizeGenerator;

		if (randomSeed.equals(Attribute.VALUE_NONE)) {
			fRandom = new Random();
		} else {
			fRandom = new Random(Long.parseLong(randomSeed));
		}
	}

	// ----------------------------------------------------------------------
	// IMultiTransformer interface.
	// ----------------------------------------------------------------------

	@Override
	public void execute(StreamProvider p) throws IOException {
		// Loads the graph.
		fGraph = LightweightStaticGraph.load(GraphCodecHelper
				.uncheckedCreateDecoder(p.input(Inputs.graph), fDecoder));

		// Loads the unit experiments.
		UnitExperimentReader reader = new UnitExperimentReader(
				p.input(Inputs.experiments), fGraph);
		fExperiments = reader.load();
		System.err.println("Loaded [" + fExperiments.size()
				+ "] unit experiments.");

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
		System.err.println("Load simulator will use ["
				+ fExecutor.getCorePoolSize() + "] threads.");

		emmitHeader();

		ProgressTracker tracker = Progress.newTracker(
				"Running load simulations", fExperiments.values().size());
		tracker.startTask();
		Iterator<UnitExperiment> it = fExperiments.values().iterator();
		while (it.hasNext()) {
			acquireCore();
			if (fExecutor.isTerminating()) {
				break;
			}
			UnitExperiment experiment = it.next();
			IScheduler scheduler = createScheduler(experiment);
			IMessageSizeGenerator generator = createSizeGenerator();
			ExperimentRunner runner = new ExperimentRunner(experiment,
					scheduler, this, generator, fPrintMode == PrintMode.all);
			fExecutor.submit(runner);
			// Well, submitting is not really progress, but it's ok.
			tracker.tick();
		}

		System.err.print("Done. Shut down... ");
		fExecutor.shutdown();
		System.err.println("[OK]");
	}

	// ----------------------------------------------------------------------

	private IScheduler createScheduler(UnitExperiment experiment) {
		HashMap<String, Object> config = baseConfig();
		config.put(IScheduler.ROOT, experiment);
		return (IScheduler) create(fScheduler, config);
	}

	// ----------------------------------------------------------------------

	private IMessageSizeGenerator createSizeGenerator() {
		HashMap<String, Object> config = baseConfig();
		return (IMessageSizeGenerator) create(fSizeGenerator, config);
	}

	// ----------------------------------------------------------------------

	private Object create(String className, HashMap<String, Object> config) {
		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			ObjectCreator creator = new ObjectCreator(
					(Class<? extends Object>) Class.forName(className),
					CompositeResolver.compositeResolver(new HashMapResolver(
							config), fResolver));
			return creator.create("");
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------

	private HashMap<String, Object> baseConfig() {
		HashMap<String, Object> config = new HashMap<String, Object>();
		config.put(IScheduler.RANDOM, fRandom);
		config.put(IScheduler.PARENT, this);
		return config;
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

	// ----------------------------------------------------------------------
	// Callbacks.
	// ----------------------------------------------------------------------

	public void printSummary(TaskResult results) {
		for (MessageStatistics statistic : results.statistics) {
			if (!shouldPrintData(results.root.id(), statistic.id)) {
				continue;
			}
			StringBuffer buffer = new StringBuffer();
			buffer.append(results.root.id());
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
			buffer.append(" ");
			buffer.append(results.duration);

			synchronizedPrint(buffer.toString());
		}
	}

	// ----------------------------------------------------------------------

	private void emmitHeader() {
		System.out.println("experiment_id node_id tx_tot rx_tot"
				+ " tx_min tx_max tx_avg tx_var"
				+ " rx_min rx_max rx_avg rx_var" + " duration");
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
		switch (fSimMode) {
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
		Future<TaskResult> future = (Future<TaskResult>) r;

		TaskResult results = null;

		try {
			results = future.get();
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

		fLoadSim.printSummary(results);
	}
}
