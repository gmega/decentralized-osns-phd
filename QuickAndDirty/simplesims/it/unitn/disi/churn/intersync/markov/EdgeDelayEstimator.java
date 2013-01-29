package it.unitn.disi.churn.intersync.markov;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * Takes a set of graph root ids and produces a file with all weights.
 * 
 * @author giuliano
 */
@AutoConfig
public class EdgeDelayEstimator implements ITransformer {

	private final GraphConfigurator fConfigurator;

	private final ExperimentReader fReader;

	public EdgeDelayEstimator(@Attribute(Attribute.AUTO) IResolver resolver) {
		fConfigurator = ObjectCreator.createInstance(GraphConfigurator.class,
				"", resolver);

		fReader = new ExperimentReader("id");
		ObjectCreator
				.fieldInject(ExperimentReader.class, fReader, "", resolver);
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		IGraphProvider provider = fConfigurator.graphProvider();

		FileOutputStream oStream = null;

		try {
			TableWriter writer = new TableWriter(oup, "id", "source", "target",
					"delay");

			Iterator<Experiment> it = fReader.iterator(provider);

			while (it.hasNext()) {
				Experiment exp = it.next();
				IndexedNeighborGraph egonet = provider.subgraph(exp.root);
				int[] ids = provider.verticesOf(exp.root);

				for (int i = 0; i < egonet.size(); i++) {
					for (int j = 0; j < egonet.degree(i); j++) {
						int neighbor = egonet.getNeighbor(i, j);
						
						writer.set("id", exp.root);
						writer.set("source", ids[i]);
						writer.set("target", ids[neighbor]);
						writer.set(
								"delay",
								edgeDelay(1.0 / exp.lis[i],
										1.0 / exp.lis[neighbor],
										2.0 / exp.dis[i],
										2.0 / exp.dis[neighbor]));
						writer.emmitRow();
						
					}
				}
			}
		} finally {
			if (oStream != null) {
				oStream.close();
			}
		}
	}

	private double edgeDelay(double lu, double lv, double du, double dv) {
		double firstHitting = ((du + lu) * (du + dv + lv))
				/ (du * dv * (du + lu + dv + lv));
		double stableState = (lv) / (lv + dv);
		return stableState * firstHitting;
	}
}
