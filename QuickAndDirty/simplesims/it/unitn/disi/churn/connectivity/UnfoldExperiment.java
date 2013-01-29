package it.unitn.disi.churn.connectivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.IndexedReader;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.churn.diffusion.graph.BranchingGraphGenerator;
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
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.tabular.TableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

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

	private final IndexedReader fWeightReader;

	@Attribute("k")
	private int fK;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("repeats")
	private int fRepeats;

	@Attribute("cores")
	private int fCores;

	@Attribute("coupled")
	private boolean fCoupled;

	public UnfoldExperiment(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("weights") String weights,
			@Attribute("weight-index") String weightIndex) throws Exception {
		fYaoConfig = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);
		fProvider = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver).graphProvider();
		fReader = new ExperimentReader("id");
		ObjectCreator
				.fieldInject(ExperimentReader.class, fReader, "", resolver);
		fSchedule = SchedulerFactory.getInstance()
				.createScheduler(resolver, "");

		fWeightReader = IndexedReader.createReader(new File(weightIndex),
				new File(weights));
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

		TableWriter writer = new TableWriter(System.out, "id", "source",
				"target", "paths", "delay");
		while ((id = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			Experiment experiment = fReader.readExperiment(id, fProvider);
			IndexedNeighborGraph original = fProvider.subgraph(experiment.root);
			int[] ids = fProvider.verticesOf(experiment.root);

			double[][] weights = readWeights(experiment.root, ids, original);
			Integer source = MiscUtils.indexOf(ids,
					Integer.parseInt(experiment.attributes.get("source")));
			Integer target = MiscUtils.indexOf(ids,
					Integer.parseInt(experiment.attributes.get("target")));

			// Unfolds the top-k graph.
			ITopKEstimator estimator = TEExperimentHelper.EDGE_DISJOINT.call(
					original, weights);

			ArrayList<? extends PathEntry> paths = estimator.topKShortest(
					source, target, fK);

			Pair<IndexedNeighborGraph, int[]> result = BranchingGraphGenerator
					.branchingGraph(paths.toArray(new PathEntry[paths.size()]));

			int[] map = result.b;
			IndexedNeighborGraph unfolded = result.a;

			System.err.println("Unfolded graph " + experiment.root + ". Size: "
					+ original.size() + " => " + unfolded.size() + ".");

			Pair<Integer, int[]> idxMap = fCoupled ? indexMap(map) : null;

			// Fullsims it:
			List<INodeMetric<Double>> metrics = helper
					.bruteForceSimulateMulti(unfolded, 0, 0,
							remap(map, experiment.lis),
							remap(map, experiment.dis), ids,
							fCoupled ? idxMap.b : null);

			INodeMetric<Double> metric = metrics.get(0);

			writer.set("id", experiment.root);
			writer.set("source", ids[source]);
			writer.set("target", ids[target]);
			writer.set("paths", paths.size());
			writer.set("split", idxMap.a);
			writer.set("delay", metric.getMetric(1));

			writer.emmitRow();
		}
	}

	/**
	 * Index map returns and array with node mappings. If map[i] = j, this means
	 * that nodes i and j are actually the same.
	 * 
	 * @param map
	 * @return
	 */
	private Pair<Integer, int[]> indexMap(final int[] map) {
		int[] idxMap = new int[map.length];
		int remapped = 0;
		for (int i = 0; i < map.length; i++) {
			// Returns the *first index* of map[i].
			int idx = MiscUtils.indexOf(map, map[i]);
			idxMap[i] = idx;
			if (idx != i) {
				System.err.println("Remapped vertex " + i + " to " + idx + ".");
				remapped++;
			}

		}

		return new Pair<Integer, int[]>(remapped, idxMap);
	}

	private double[][] readWeights(Integer id, int[] ids,
			IndexedNeighborGraph graph) throws IOException {
		if (fWeightReader.select(id) == null) {
			throw new NoSuchElementException();
		}
		MatrixReader reader = new MatrixReader(fWeightReader.getStream(), "V0",
				"V1", "V2", "V3");

		double[][] weights = reader.read(ids);
		LightweightStaticGraph lsg = (LightweightStaticGraph) graph;
		if (lsg.edgeCount() * 2 != reader.lastRead()) {
			throw new IllegalStateException();
		}
		return weights;
	}

	private double[] remap(int[] map, double[] attribute) {
		double[] mapped = new double[map.length];
		for (int i = 0; i < mapped.length; i++) {
			mapped[i] = attribute[map[i]];
		}
		return mapped;
	}

}
