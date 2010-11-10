package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.LightweightStaticGraph;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
import peersim.config.resolvers.DefaultValueResolver;
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
	 * Field separator char.
	 */
	private static final String FS = " ";

	/**
	 * {@link StreamProvider} input keys.
	 */
	public static enum Inputs {
		graph, experiments;
	}

	/**
	 * {@link StreamProvider} output keys.
	 */
	public static enum Ouputs {
		load;
	}

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	/**
	 * The {@link IndexedNeighborGraph} containing the neighbor dependencies
	 * between the unit experiments.
	 */
	private volatile IndexedNeighborGraph fGraph;

	private String fDecoder;

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
			@Attribute("percentage") double percentage,
			@Attribute("cores") int cores,
			@Attribute(value = "decoder", defaultValue = Attribute.VALUE_NULL) String decoder) {

		fExecutor = new CallbackThreadPoolExecutor(cores, this);
		fSema = new Semaphore(cores);
		fPercentage = percentage;
		fRandom = new Random();
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
		fStream = new PrintStream(p.output(Ouputs.load));

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
		while (it.hasNext() && !fExecutor.isTerminating()) {
			UnitExperiment experiment = it.next();
			PercentageRandomScheduler scheduler = new PercentageRandomScheduler(
					this, experiment.id(), fPercentage, fRandom);
			ExperimentRunner runner = new ExperimentRunner(experiment.id(),
					scheduler, this);
			fExecutor.submit(runner);
			acquireCore();
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
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		String line;
		while ((line = reader.readLine()) != null) {
			String[] lineParts = line.split(FS);
			// Reads the fields.
			int id = Integer.parseInt(lineParts[0]);
			int nodeId = Integer.parseInt(lineParts[1]);
			int sent = Integer.parseInt(lineParts[2]);
			int received = Integer.parseInt(lineParts[3]);

			UnitExperiment experiment = experiments.get(id);
			if (experiment == null) {
				experiment = new UnitExperiment(id, graph.degree(id));
				experiments.put(id, experiment);
			}

			experiment.addData(nodeId, sent, received);
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

	public synchronized void synchronizedPrint(int root,
			Collection<? extends MessageStatistics> collection) {
		for (MessageStatistics statistic : collection) {
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

			fStream.println(buffer.toString());
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

	@Override
	public void synchronizedPrint(String data) {
		fStream.println(data.toString());
	}

	// ----------------------------------------------------------------------

	@Override
	public UnitExperiment unitExperiment(int index) {
		if (!fExperiments.containsKey(index)) {
			fExecutor.shutdown();
			System.err.println("ERROR: Missing unit experiment data " + index
					+ ".");
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
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} finally {
			fLoadSim.releaseCore();
		}

		fLoadSim.synchronizedPrint(statistics.a, statistics.b);
	}
}
