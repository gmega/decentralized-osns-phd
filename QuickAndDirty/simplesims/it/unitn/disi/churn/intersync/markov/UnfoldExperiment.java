package it.unitn.disi.churn.intersync.markov;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.MatrixReader;
import it.unitn.disi.churn.connectivity.TEExperimentHelper;
import it.unitn.disi.distsim.scheduler.generators.ISchedule;
import it.unitn.disi.distsim.scheduler.generators.IScheduleIterator;
import it.unitn.disi.distsim.scheduler.generators.Schedulers;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.tabular.TableWriter;
import it.unitn.disi.utils.tabular.minidb.IndexedReader;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

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
		fSchedule = Schedulers.get(resolver.getString("", "scheduler.type"),
				resolver);

		fWeightReader = IndexedReader.createReader(new File(weightIndex),
				new File(weights));

	}

	@Override
	public void run() {
		ExoticSimHelper helper = new ExoticSimHelper(fRepeats, fBurnin,
				fYaoConfig, new TEExperimentHelper(fYaoConfig, fCores,
						fRepeats, fBurnin));
		try {
			run0(helper);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			try {
				helper.getDelegate().shutdown(true);
			} catch (InterruptedException e) {
				// Swallows.
			}
		}
	}

	private void run0(ExoticSimHelper helper) throws Exception {
		IScheduleIterator it = fSchedule.iterator();
		Integer id;

		TableWriter writer = new TableWriter(System.out, "id", "source",
				"target", "delay");

		while ((id = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			Experiment experiment = fReader.readExperiment(id, fProvider);
			IndexedNeighborGraph original = fProvider.subgraph(experiment.root);
			int[] ids = fProvider.verticesOf(experiment.root);

			double[][] weights = readWeights(experiment.root, ids, original);
			Integer source = MiscUtils.indexOf(ids,
					Integer.parseInt(experiment.attributes.get("source")));
			Integer target = MiscUtils.indexOf(ids,
					Integer.parseInt(experiment.attributes.get("target")));

			double delay = helper.unfoldedGraphSimulation(original,
					TEExperimentHelper.EDGE_DISJOINT, source, target, weights,
					experiment.lis, experiment.dis, fK, fCoupled);

			writer.set("id", experiment.root);
			writer.set("source", ids[source]);
			writer.set("target", ids[target]);
			writer.set("delay", delay);

			writer.emmitRow();
		}
	}

	private double[][] readWeights(Integer id, int[] ids,
			IndexedNeighborGraph graph) throws IOException {
		if (fWeightReader.select(id) == null) {
			throw new NoSuchElementException();
		}
		MatrixReader reader = new MatrixReader(fWeightReader.getReader(), "V0",
				"V1", "V2", "V3");

		double[][] weights = reader.read(ids);
		LightweightStaticGraph lsg = (LightweightStaticGraph) graph;
		if (lsg.edgeCount() * 2 != reader.lastRead()) {
			throw new IllegalStateException();
		}
		return weights;
	}

}
