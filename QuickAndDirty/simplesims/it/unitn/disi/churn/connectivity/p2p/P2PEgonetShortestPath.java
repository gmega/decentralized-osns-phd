package it.unitn.disi.churn.connectivity.p2p;

import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.DunnTopK;
import it.unitn.disi.graph.analysis.DunnTopK.Mode;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.extras.am.util.IncrementalStatsFreq;

@AutoConfig
public class P2PEgonetShortestPath implements ITransformer {

	private final ShortestPathTask SHUTDOWN = new ShortestPathTask(-1, -1,
			null, null, null, null, -1);

	private GraphConfigurator fConfig;

	@Attribute("pairwise_estimates")
	private String fPairwiseEstimates;

	@Attribute("k")
	private int fK;

	public P2PEgonetShortestPath(@Attribute(Attribute.AUTO) IResolver resolver) {
		fConfig = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, final OutputStream oup)
			throws Exception {

		MatrixReader reader = new MatrixReader(new FileInputStream(
				fPairwiseEstimates), "id", "source", "target", "ttc");

		IGraphProvider provider = fConfig.graphProvider();

		final LinkedBlockingQueue<ShortestPathTask> toPrint = new LinkedBlockingQueue<ShortestPathTask>();

		Thread printer = new Thread(new Runnable() {
			@Override
			public void run() {
				TableWriter writer = new TableWriter(new PrefixedWriter("S:",
						oup), "id", "source", "target", "estimate", "avglength");

				TableWriter lwriter = new TableWriter(new PrefixedWriter("L:",
						oup), "id", "length", "freq");

				while (true) {
					ShortestPathTask result = null;
					try {
						result = toPrint.take();
						if (result == SHUTDOWN) {
							System.err.println("Printer thread terminating.");
							break;
						}
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}

					for (int i = 0; i < result.fGraph.size(); i++) {
						writer.set("id", result.root);
						writer.set("source", result.ids[result.source]);
						writer.set("target", result.ids[i]);
						writer.set("estimate", result.mindists()[i]);
						writer.set("avglength", result.avgLength(i));
						writer.emmitRow();
					}

					IncrementalStatsFreq freq = result.lengthDist();
					for (int i = (int) freq.getMin(); i < (int) freq.getMax(); i++) {
						lwriter.set("id", result.root);
						lwriter.set("length", i);
						lwriter.set("freq", freq.getFreq(i));
						lwriter.emmitRow();
					}
				}
			}
		});

		printer.start();

		final CallbackThreadPoolExecutor<ShortestPathTask> executor = new CallbackThreadPoolExecutor<ShortestPathTask>(
				Runtime.getRuntime().availableProcessors(),
				new IExecutorCallback<ShortestPathTask>() {

					@Override
					public void taskFailed(Future<ShortestPathTask> task,
							Throwable ex) {
						ex.printStackTrace();
					}

					@Override
					public void taskDone(ShortestPathTask result) {
						try {
							toPrint.offer(result, Long.MAX_VALUE, TimeUnit.DAYS);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				});

		while (reader.hasNext()) {
			int root = Integer.parseInt(reader.currentRoot());
			IndexedNeighborGraph egoNet = provider.subgraph(root);
			int[] ids = provider.verticesOf(root);
			double[][] w = reader.read(ids);

			String err;
			if ((err = check(egoNet, root, w, ids)) != null) {
				System.err.println("Incomplete information: \n " + err);
				continue;
			}

			for (int i = 0; i < egoNet.size(); i++) {
				executor.submit(new ShortestPathTask(root, i, w, egoNet, ids,
						Mode.EdgeDisjoint, fK));
			}
		}

		System.err.println("Queue shutdown request.");
		executor.shutdown();
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		toPrint.offer(SHUTDOWN, Long.MAX_VALUE, TimeUnit.DAYS);
		printer.join();

	}

	private String check(IndexedNeighborGraph egoNet, int root, double[][] w,
			int[] ids) {
		for (int i = 0; i < egoNet.size(); i++) {
			for (int j = 0; j < egoNet.size(); j++) {
				if (i == j) {
					continue;
				}
				if (egoNet.isEdge(i, j)) {
					if (w[i][j] == Double.MAX_VALUE
							|| w[j][i] == Double.MAX_VALUE) {
						return root + " " + ids[i] + " " + ids[j];
					}
				}
			}
		}
		return null;
	}

	static class ShortestPathTask implements Callable<ShortestPathTask> {

		public final int root;

		public final int source;

		public final int[] ids;

		public final double[][] fWeights;

		private double[] fMinDists;
		
		private double[] fAvgLengths;

		public final IndexedNeighborGraph fGraph;

		private IncrementalStatsFreq fStats;

		private final Mode fMode;

		private final int fK;

		public ShortestPathTask(int root, int source, double[][] weights,
				IndexedNeighborGraph graph, int[] ids, Mode mode, int k) {
			fGraph = graph;
			this.source = source;
			fWeights = weights;
			this.root = root;
			this.ids = ids;
			fMode = mode;
			fK = k;
		}

		@Override
		public ShortestPathTask call() throws Exception {

			synchronized (this) {
				fStats = new IncrementalStatsFreq();
			}

			double[] minDists = new double[fGraph.size()];
			double[] avgLength = new double[fGraph.size()];
			
			minDists[source] = 0;
			
			DunnTopK dtk = new DunnTopK(fGraph, fWeights, fMode);
			for (int i = 0; i < fGraph.size(); i++) {
				if (i == source) {
					continue;
				}
				ArrayList<PathEntry> paths = dtk.topKShortest(source, i, fK);
				for (PathEntry entry : paths) {
					int length = entry.path.length - 1;
					fStats.add(length);
					avgLength[i] += length;
				}
				avgLength[i] /= paths.size();
				minDists[i] = paths.get(0).cost;
			}

			synchronized (this) {
				fMinDists = minDists;
				fAvgLengths = avgLength;
			}

			return this;
		}

		public synchronized double[] mindists() {
			return fMinDists;
		}

		public synchronized IncrementalStatsFreq lengthDist() {
			return fStats;
		}
		
		public synchronized double avgLength(int i) {
			return fAvgLengths[i];
		}
	}
}
