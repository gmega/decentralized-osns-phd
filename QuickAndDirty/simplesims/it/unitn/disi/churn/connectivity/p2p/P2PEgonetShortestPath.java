package it.unitn.disi.churn.connectivity.p2p;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.utils.CallbackThreadPoolExecutor;
import it.unitn.disi.utils.IExecutorCallback;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class P2PEgonetShortestPath implements ITransformer {

	private final ShortestPathTask SHUTDOWN = new ShortestPathTask(-1, -1,
			null, null, null);

	private GraphConfigurator fConfig;

	public P2PEgonetShortestPath(@Attribute(Attribute.AUTO) IResolver resolver) {
		fConfig = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
	}

	@Override
	public void execute(InputStream is, final OutputStream oup)
			throws Exception {
		MatrixReader reader = new MatrixReader(is, "id", "source", "target",
				"ttc");

		IGraphProvider provider = fConfig.graphProvider();

		final LinkedBlockingQueue<ShortestPathTask> toPrint = new LinkedBlockingQueue<ShortestPathTask>();

		Thread printer = new Thread(new Runnable() {
			@Override
			public void run() {
				TableWriter writer = new TableWriter(new PrintStream(oup),
						"id", "source", "target", "estimate");
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
						writer.emmitRow();
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
				executor.submit(new ShortestPathTask(root, i, w, egoNet, ids));
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

		public final IndexedNeighborGraph fGraph;

		private double[] fMinDists;

		public ShortestPathTask(int root, int source, double[][] weights,
				IndexedNeighborGraph graph, int[] ids) {
			fGraph = graph;
			this.source = source;
			fWeights = weights;
			this.root = root;
			this.ids = ids;
		}

		@Override
		public ShortestPathTask call() throws Exception {
			double[] mindists = new double[fGraph.size()];
			int[] previous = new int[fGraph.size()];

			GraphAlgorithms.dijkstra(fGraph, source, fWeights, mindists,
					previous);

			synchronized (this) {
				fMinDists = mindists;
			}
			return this;
		}

		public synchronized double[] mindists() {
			return fMinDists;
		}

	}
}
