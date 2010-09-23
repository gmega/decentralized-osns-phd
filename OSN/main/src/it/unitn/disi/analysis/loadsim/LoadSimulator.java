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

@AutoConfig
public class LoadSimulator implements IMultiTransformer {

	private static final String FS = " ";

	enum Inputs {
		graph(0), experiments(1);

		final int index;

		Inputs(int index) {
			this.index = index;
		}
	}

	private final ThreadPoolExecutor fExecutor;

	private volatile UnitExperiment[] fExperiments;

	private volatile IndexedNeighborGraph fGraph;

	private final double fPercentage;

	private final Random fRandom;

	private PrintStream fStream;

	public LoadSimulator(@Attribute double percentage, @Attribute int cores) {

		fPercentage = percentage;
		fRandom = new Random();
		fExecutor = new ThreadPoolExecutor(cores, cores, 0L,
				TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1)) {

			@Override
			@SuppressWarnings("unchecked")
			public void afterExecute(Runnable r, Throwable t) {
				Future<Pair<Integer, Collection<? extends MessageStatistics>>> future = 
					(Future<Pair<Integer, Collection<? extends MessageStatistics>>>) r;
				
				Pair<Integer, Collection<? extends MessageStatistics>> pair = null;
				
				try {
					pair = future.get();
				} catch (ExecutionException ex) {
					throw new RuntimeException(ex);
				} catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				
				LoadSimulator.this.synchronizedPrint(pair.a, pair.b);
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
		runExperiments();
	}

	private void runExperiments() {
		for (UnitExperiment experiment : fExperiments) {
			PercentageRandomScheduler scheduler = new PercentageRandomScheduler(
					this, experiment.id(), fPercentage, fRandom);
			ExperimentRunner runner = new ExperimentRunner(experiment.id(), scheduler, this);
			fExecutor.submit(runner);
		}
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

		return experiments;
	}

	public synchronized void synchronizedPrint(int rootId,
			Collection<? extends MessageStatistics> collection) {

	}

	public synchronized void synchronizedPrint(String data) {
		fStream.println(data.toString());
	}

	public UnitExperiment unitExperiment(int index) {
		return fExperiments[index];
	}

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
