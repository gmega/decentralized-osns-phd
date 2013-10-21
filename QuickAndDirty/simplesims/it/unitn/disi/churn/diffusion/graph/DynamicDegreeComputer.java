package it.unitn.disi.churn.diffusion.graph;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.PartialLoader;
import it.unitn.disi.utils.tabular.TableWriter;

@AutoConfig
public class DynamicDegreeComputer implements Runnable {

	private int fExperiments;

	private final ExperimentReader fReader;

	private PartialLoader fProvider;

	public DynamicDegreeComputer(
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute("n") int experiments) 
					throws Exception {

		fProvider = (PartialLoader) ObjectCreator.createInstance(
				GraphConfigurator.class, "", resolver).graphProvider();

		fReader = new ExperimentReader("id");
		fExperiments = experiments;
		ObjectCreator
				.fieldInject(ExperimentReader.class, fReader, "", resolver);
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void run0() throws Exception {

		TableWriter writer = new TableWriter(System.out, "id", "source",
				"target", "realdegree", "egodegree", "dynamicdegree");

		for (int i = 2; i <= fExperiments + 1; i++) {
			Experiment experiment = fReader.readExperiment(i, fProvider);

			IndexedNeighborGraph graph = fProvider.subgraph(experiment.root);
			int[] ids = fProvider.verticesOf(experiment.root);
			int source = Integer.parseInt(experiment.attributes.get("node"));

			double[] ai = availabilities(experiment);

			for (int j = 0; j < graph.size(); j++) {
				writer.set("id", experiment.root);
				writer.set("source", source);
				writer.set("target", ids[j]);
				writer.set("realdegree", fProvider.size(ids[j]) - 1);
				writer.set("egodegree", graph.degree(j));
				writer.set("dynamicdegree", dynamicDegree(graph, j, ai));
				writer.emmitRow();
			}
		}

	}

	private double dynamicDegree(IndexedNeighborGraph graph, int node,
			double[] ai) {
		double avg = 0;
		int degree = graph.degree(node);
		for (int i = 0; i < degree; i++) {
			int neighbor = graph.getNeighbor(node, i);
			avg += ai[neighbor];
		}

		return avg;
	}

	private double[] availabilities(Experiment experiment) {
		double[] li = experiment.lis;
		double[] di = experiment.dis;
		double[] ai = new double[experiment.lis.length];

		for (int i = 0; i < ai.length; i++) {
			ai[i] = li[i] / (li[i] + di[i]);
		}

		return ai;
	}

}
