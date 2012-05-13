package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.churn.config.YaoChurnConfigurator;
import it.unitn.disi.churn.simulator.TaskExecutor;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.SchedulerFactory;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.util.IncrementalStats;

@AutoConfig
public class DiffusionExperiment implements ITransformer {

	private GraphConfigurator fConfig;

	@Attribute("period")
	private double fPeriod;

	@Attribute("burnin")
	private double fBurnin;

	@Attribute("selector")
	private String fSelector;

	@Attribute("repeats")
	private int fRepeats;

	@Attribute("cores")
	private int fCores;

	private YaoChurnConfigurator fYaoChurn;

	private IResolver fResolver;

	private ExperimentReader fReader;

	private TaskExecutor fExecutor;

	public DiffusionExperiment(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(value = "churn", defaultValue = "false") boolean churn) {
		fConfig = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);

		if (churn) {
			fYaoChurn = ObjectCreator.createInstance(
					YaoChurnConfigurator.class, "", resolver);
		}

		fReader = new ExperimentReader("id");
		ObjectCreator
				.fieldInject(ExperimentReader.class, fReader, "", resolver);

		fResolver = resolver;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		fExecutor = new TaskExecutor(fCores);
		IGraphProvider provider = fConfig.graphProvider();
		IScheduleIterator it = SchedulerFactory.getInstance()
				.createScheduler(fResolver, "").iterator();

		TableWriter writer = new TableWriter(new PrefixedWriter("ES:", oup),
				"id", "source", "target", "lsum");

		TableWriter cCore = new TableWriter(new PrefixedWriter("CO:", oup),
				"id", "source", "lsum", "size");

		Integer row;
		while ((row = (Integer) it.nextIfAvailable()) != IScheduleIterator.DONE) {
			Experiment experiment = fReader.readExperiment(row, provider);

			IndexedNeighborGraph graph = provider.subgraph(experiment.root);
			int source = Integer.parseInt(experiment.attributes.get("node"));

			int[] ids = provider.verticesOf(experiment.root);
			double[] latencies = runExperiments(experiment,
					MiscUtils.indexOf(ids, source), ids, graph, fRepeats, cCore);

			for (int i = 0; i < latencies.length; i++) {
				writer.set("id", experiment.root);
				writer.set("source", source);
				writer.set("target", ids[i]);
				writer.set("lsum", latencies[i]);
				writer.emmitRow();
			}
		}
	}

	private double[] runExperiments(Experiment experiment, int source,
			int[] ids, IndexedNeighborGraph graph, int repetitions,
			TableWriter core) throws Exception {
		double[] latencies = new double[graph.size()];

		fExecutor.start(experiment.toString() + ", source: " + source
				+ " size: " + graph.size(), repetitions);
		for (int i = 0; i < repetitions; i++) {
			fExecutor.submit(new SimulationTask(fBurnin, fPeriod, experiment,
					fYaoChurn, source, fSelector, graph, new Random()));
		}

		IncrementalStats stats = new IncrementalStats();

		for (int i = 0; i < repetitions; i++) {
			stats.reset();

			Object value = fExecutor.consume();
			if (value instanceof Throwable) {
				((Throwable) value).printStackTrace();
			} else {
				HFlood[] result = (HFlood[]) value;
				double base = result[source].latency();
				for (int j = 0; j < result.length; j++) {
					double latency = result[j].latency() - base;
					latencies[j] += latency;
					if (result[j].isPartOfConnectedCore()) {
						stats.add(latency);
					}
				}
			}
			
			if (fYaoChurn != null) {
				core.set("id", experiment.root);
				core.set("source", ids[source]);
				core.set("lsum", stats.getSum());
				core.set("lsqrsum", stats.getSqrSum());
				core.set("size", stats.getN());
				core.emmitRow();
			}
		}

		return latencies;
	}
}
