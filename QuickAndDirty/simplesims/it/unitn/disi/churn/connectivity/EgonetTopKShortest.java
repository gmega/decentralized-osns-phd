package it.unitn.disi.churn.connectivity;

import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.LawlerTopK;
import it.unitn.disi.graph.analysis.LawlerTopK.PathEntry;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.util.IncrementalStats;

@AutoConfig
public class EgonetTopKShortest implements ITransformer {

	private GraphConfigurator fGraphConfig;

	@Attribute("k")
	private int fK;

	@Attribute("samples")
	private String fSamples;

	@Attribute("printpaths")
	private boolean fPrintPaths;

	public EgonetTopKShortest(@Attribute(Attribute.AUTO) IResolver resolver) {
		fGraphConfig = ObjectCreator.createInstance(GraphConfigurator.class,
				"", resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {

		TableWriter aggregateWriter = new TableWriter(new PrintWriter(
				new PrefixedWriter("SM:", new OutputStreamWriter(System.out))),
				"id", "source", "target", "n", "avg", "var", "std", "coef");

		TableWriter pathWriter = new TableWriter(new PrintWriter(
				new PrefixedWriter("SP:", new OutputStreamWriter(System.out))),
				"id", "source", "target", "pid", "plength", "cost", "path");

		MatrixReader reader = new MatrixReader(is, "id", "source", "target",
				"ttc");
		Set<String> samples = parseSamples(fSamples);
		IGraphProvider provider = fGraphConfig.graphProvider();

		while (reader.hasNext()) {
			String strRoot = reader.currentRoot();
			if (!samples.contains(strRoot)) {
				reader.skipCurrent();
				continue;
			}

			int root = Integer.parseInt(strRoot);
			int[] ids = provider.verticesOf(root);
			double[][] w = reader.read(ids);

			IndexedNeighborGraph ego = provider.subgraph(root);
			LawlerTopK tpk = new LawlerTopK(ego, w);

			IncrementalStats stats = new IncrementalStats();

			ProgressTracker tracker = Progress.newTracker("processing",
					w.length * (w.length - 1));
			tracker.startTask();

			for (int i = 0; i < w.length; i++) {
				for (int j = 0; j < w.length; j++) {
					if (i == j) {
						continue;
					}
					ArrayList<PathEntry> paths = tpk.topKShortest(i, j, fK);
					stats.reset();
					for (int k = 0; k < paths.size(); k++) {
						PathEntry entry = paths.get(k);
						stats.add(entry.cost);
						if (fPrintPaths) {
							pathWriter.set("id", root);
							pathWriter.set("source", ids[i]);
							pathWriter.set("target", ids[j]);
							pathWriter.set("pid", k);
							pathWriter.set("plength", entry.path.length);
							pathWriter.set("cost", entry.cost);
							pathWriter.set("path", Arrays.toString(entry.path));
							pathWriter.emmitRow();
						}
					}

					aggregateWriter.set("id", root);
					aggregateWriter.set("source", ids[i]);
					aggregateWriter.set("target", ids[j]);
					aggregateWriter.set("n", paths.size());
					aggregateWriter.set("avg", stats.getAverage());
					aggregateWriter.set("var", stats.getVar());
					aggregateWriter.set("std", stats.getStD());
					aggregateWriter.set("coef",
							(stats.getStD() / stats.getAverage()));
					aggregateWriter.emmitRow();
					tracker.tick();
				}
			}
		}
	}

	private Set<String> parseSamples(String smpl) {
		String[] parts = smpl.split(",");
		HashSet<String> samples = new HashSet<String>();
		for (int i = 0; i < parts.length; i++) {
			samples.add(parts[i]);
		}
		return samples;
	}

	static class TOPKTask implements Callable<ArrayList<PathEntry>> {

		private final LawlerTopK fEstimator;

		private final int fI;

		private final int fJ;

		private final int fK;

		public TOPKTask(int i, int j, int k, LawlerTopK estimator) {
			fI = i;
			fJ = j;
			fK = k;
			fEstimator = estimator;
		}

		@Override
		public ArrayList<PathEntry> call() throws Exception {
			return fEstimator.topKShortest(fI, fJ, fK);
		}

	}
}
