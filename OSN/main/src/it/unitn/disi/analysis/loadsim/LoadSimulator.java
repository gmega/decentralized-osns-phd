package it.unitn.disi.analysis.loadsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.LightweightStaticGraph;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.graph.codecs.AdjListGraphDecoder;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;
import peersim.util.IncrementalStats;

@AutoConfig
public class LoadSimulator implements IMultiTransformer, ILoadSim {

	private static final String FS = " ";

	public static enum Inputs {
		graph, experiments;
	}
	
	public static enum Ouputs {
		load;
	}
	
	
	private final Semaphore fSema;

	private final ThreadPoolExecutor fExecutor;

	private volatile Map<Integer, UnitExperiment> fExperiments;

	private volatile IndexedNeighborGraph fGraph;

	private final double fPercentage;

	private final Random fRandom;

	private PrintStream fStream;

	public LoadSimulator(
			@Attribute("percentage") double percentage, 
			@Attribute("cores") int cores ) {

		fPercentage = percentage;
		fRandom = new Random();
		fSema = new Semaphore(cores);
		fExecutor = new ThreadPoolExecutor(cores, cores, 0L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()) {

			@Override
			@SuppressWarnings("unchecked")
			public void afterExecute(Runnable r, Throwable t) {
				Future<Pair<Integer, Collection<? extends MessageStatistics>>> future = 
					(Future<Pair<Integer, Collection<? extends MessageStatistics>>>) r;
				
				Pair<Integer, Collection<? extends MessageStatistics>> statistics = null;

				try {
					statistics = future.get();
				} catch (ExecutionException ex) {
					System.err.println("Error while running task.");
					ex.printStackTrace();
					fExecutor.shutdown();
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				} finally {
					fSema.release();
				}
				
				LoadSimulator.this
						.synchronizedPrint(statistics.a, statistics.b);
			}
		};
	}

	@Override
	public void execute(StreamProvider p)
			throws IOException {

		// Loads the graph.
		fGraph = LightweightStaticGraph.load(new AdjListGraphDecoder(
				p.input(Inputs.graph)));

		// Loads the unit experiments.
		fExperiments = loadUnitExperiments(fGraph,
				p.input(Inputs.experiments));

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

	private void runExperiments() throws InterruptedException {
		System.err.println("Now running simulations using ["
				+ fExecutor.getCorePoolSize() + "] threads.");
		
		Iterator<UnitExperiment> it = fExperiments.values().iterator();
		while(it.hasNext() && !fExecutor.isTerminating()) {
			UnitExperiment experiment = it.next();
			PercentageRandomScheduler scheduler = new PercentageRandomScheduler(
					this, experiment.id(), fPercentage, fRandom);
			ExperimentRunner runner = new ExperimentRunner(experiment.id(), scheduler, this);
			fExecutor.submit(runner);
			fSema.acquire();
		}
		
		System.err.print("Done. Shut down... ");
		fExecutor.shutdown();
		System.err.println("[OK]");
	}

	private Map<Integer, UnitExperiment> loadUnitExperiments(Graph graph, InputStream input)
			throws IOException {
		
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
		for(UnitExperiment experiment : experiments.values()) {
			experiment.done();
		}
		
		System.err.println("[OK]");
		return experiments;
	}

	public synchronized void synchronizedPrint(int root, Collection<? extends MessageStatistics> collection) {
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

	/* (non-Javadoc)
	 * @see it.unitn.disi.analysis.loadsim.ILoadSim#synchronizedPrint(java.lang.String)
	 */
	public void synchronizedPrint(String data) {
		fStream.println(data.toString());
	}

	/* (non-Javadoc)
	 * @see it.unitn.disi.analysis.loadsim.ILoadSim#unitExperiment(int)
	 */
	public UnitExperiment unitExperiment(int index) {
		if (!fExperiments.containsKey(index)) {
			fExecutor.shutdown();
			System.err.println("ERROR: Missing unit experiment data " + index + ".");			
		}
		return fExperiments.get(index);
	}

	/* (non-Javadoc)
	 * @see it.unitn.disi.analysis.loadsim.ILoadSim#getGraph()
	 */
	public IndexedNeighborGraph getGraph() {
		return fGraph;
	}
}
