package it.unitn.disi.analysis.loadsim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.codecs.AdjListGraphDecoder;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.graph.IndexedNeighborGraph;
import it.unitn.disi.utils.graph.LightweightStaticGraph;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;
import peersim.util.IncrementalStats;

@AutoConfig
public class LoadSimulator implements IMultiTransformer, ILoadSim {

	private static final String FS = " ";

	enum Inputs {
		graph(0), experiments(1);

		final int index;

		Inputs(int index) {
			this.index = index;
		}
	}
	
	private final Semaphore fSema;

	private final ThreadPoolExecutor fExecutor;

	private volatile UnitExperiment[] fExperiments;

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

				fSema.release();
				
				Pair<Integer, Collection<? extends MessageStatistics>> statistics = null;

				try {
					statistics = future.get();
				} catch (ExecutionException ex) {
					throw new RuntimeException(ex);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}

				LoadSimulator.this
						.synchronizedPrint(statistics.a, statistics.b);
			}
		};
	}

	@Override
	public void execute(InputStream[] istreams, OutputStream[] ostreams)
			throws IOException {

		// Loads the graph.
		fGraph = LightweightStaticGraph.load(new AdjListGraphDecoder(
				istreams[Inputs.graph.index]));

		// Loads the unit experiments.
		fExperiments = loadUnitExperiments(fGraph,
				istreams[Inputs.experiments.index]);

		// Sets up the output stream.
		fStream = new PrintStream(ostreams[0]);

		// Runs the experiments
		try {
			runExperiments();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return;
		}
	}

	private void runExperiments() throws InterruptedException {
		for (UnitExperiment experiment : fExperiments) {
			PercentageRandomScheduler scheduler = new PercentageRandomScheduler(
					this, experiment.id(), fPercentage, fRandom);
			ExperimentRunner runner = new ExperimentRunner(experiment.id(), scheduler, this);
			fExecutor.submit(runner);
			fSema.acquire();
		}
		
		fExecutor.shutdown();
	}

	private UnitExperiment[] loadUnitExperiments(Graph graph, InputStream input)
			throws IOException {
		UnitExperiment[] experiments = new UnitExperiment[graph.size()];
		for (int i = 0; i < experiments.length; i++) {
			experiments[i] = new UnitExperiment(i, graph.degree(i));
		}

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

			experiments[id].addData(nodeId, sent, received);
		}
		
		// Wraps up the experiments.
		for(UnitExperiment experiment : experiments) {
			experiment.done();
		}

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

			System.out.println(buffer.toString());
		}
	}

	private void appendStatistic(IncrementalStats stats,
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
	public synchronized void synchronizedPrint(String data) {
		fStream.println(data.toString());
	}

	/* (non-Javadoc)
	 * @see it.unitn.disi.analysis.loadsim.ILoadSim#unitExperiment(int)
	 */
	public UnitExperiment unitExperiment(int index) {
		return fExperiments[index];
	}

	/* (non-Javadoc)
	 * @see it.unitn.disi.analysis.loadsim.ILoadSim#getGraph()
	 */
	public IndexedNeighborGraph getGraph() {
		return fGraph;
	}

	@Override
	public String[] inputStreamNames() {
		return new String[] { Inputs.graph.toString(),
				Inputs.experiments.toString() };
	}

	@Override
	public String[] outputStreamNames() {
		return new String[] {};
	}
}
