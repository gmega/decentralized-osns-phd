package it.unitn.disi.churn.connectivity;

import java.util.ArrayList;
import java.util.List;

import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.distsim.scheduler.generators.ISchedule;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.ITopKEstimator;
import it.unitn.disi.graph.analysis.PathEntry;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.SparseIDMapper;
import it.unitn.disi.utils.collections.Pair;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.graph.BitMatrixGraph;

/**
 * 
 * This experiment tests the idea that unfolding a top-k graph into disjoint
 * paths and observing the delay over them provides a good approximation of the
 * delay values observed over the top-k graph itself.
 * 
 * @author giuliano
 */
@AutoConfig
public class UnfoldExperiment implements Runnable {

	private YaoChurnConfigurator fYaoConfig;

	private IGraphProvider fProvider;

	private ExperimentReader fReader;

	private ISchedule fSchedule;

	@Attribute("k")
	private int fK;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("repeats")
	private int fRepeats;

	@Attribute("cores")
	private int fCores;

	public UnfoldExperiment(@Attribute(Attribute.AUTO) IResolver resolver)
			throws Exception {
		fYaoConfig = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);
		fProvider = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver).graphProvider();
		fReader = ObjectCreator.createInstance(ExperimentReader.class, "",
				resolver);
		fSchedule = SchedulerFactory.getInstance()
				.createScheduler(resolver, "");
	}

	@Override
	public void run() {
		TEExperimentHelper helper = new TEExperimentHelper(fYaoConfig, fCores,
				fRepeats, fBurnin);
		try {
			run0(helper);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			try {
				helper.shutdown(true);
			} catch (InterruptedException e) {
				// Swallows.
			}
		}
	}

	private void run0(TEExperimentHelper helper) throws Exception {
		IScheduleIterator it = fSchedule.iterator();
		Integer id;

		while ((id = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			IndexedNeighborGraph original = fProvider.subgraph(id);
			int[] ids = fProvider.verticesOf(id);
			Experiment experiment = fReader.readExperiment(id, fProvider);
			double[][] weights = readWeights(id, ids);

			Integer source = Integer.parseInt(experiment.attributes
					.get("source"));
			Integer target = Integer.parseInt(experiment.attributes
					.get("target"));

			// Unfolds the top-k graph.
			ITopKEstimator estimator = TEExperimentHelper.EDGE_DISJOINT.call(
					original, weights);
			Pair<AbstractIDMapper, IndexedNeighborGraph> result = unfold(
					estimator, source, target);

			AbstractIDMapper mapper = result.a;
			IndexedNeighborGraph unfolded = result.b;

			// Fullsims it:
			List<INodeMetric<Double>> metrics = helper.bruteForceSimulateMulti(
					unfolded, 0, mapper.map(source),
					remap(mapper, experiment.lis),
					remap(mapper, experiment.dis), ids);

			INodeMetric<Double> metric = metrics.get(0);
			System.out.println("DL:" + metric.getMetric(mapper.map(target)));
		}
	}

	private double[][] readWeights(Integer id, int[] ids) {
		return null;
	}

	private double[] remap(AbstractIDMapper mapper, double[] attribute) {
		double[] remapped = new double[mapper.size()];
		for (int i = 0; i < remapped.length; i++) {
			remapped[i] = attribute[mapper.reverseMap(i)];
		}
		return remapped;
	}

	private Pair<AbstractIDMapper, IndexedNeighborGraph> unfold(
			ITopKEstimator estimator, Integer source, Integer target) {
		ArrayList<? extends PathEntry> paths = estimator.topKShortest(source,
				target, fK);
		int nodes = count(paths) + 2;
		BitMatrixGraph bmg = new BitMatrixGraph(nodes);
		SparseIDMapper mapper = new SparseIDMapper();

		for (PathEntry entry : paths) {
			int[] path = entry.path;
			for (int i = 1; i < path.length; i++) {
				bmg.setEdge(mapper.addMapping(path[i - 1]),
						mapper.addMapping(path[i]));
			}
		}

		return new Pair<AbstractIDMapper, IndexedNeighborGraph>(mapper,
				LightweightStaticGraph.fromGraph(bmg));
	}

	private int count(ArrayList<? extends PathEntry> paths) {
		int count = 0;
		for (PathEntry path : paths) {
			count += path.path.length - 2;
		}
		return count;
	}
}
