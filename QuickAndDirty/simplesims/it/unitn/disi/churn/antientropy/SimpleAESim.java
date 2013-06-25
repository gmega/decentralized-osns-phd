package it.unitn.disi.churn.antientropy;

import java.util.Iterator;
import java.util.Random;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.config.ExperimentReader;
import it.unitn.disi.churn.config.GraphConfigurator;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.simulator.churnmodel.yao.YaoChurnConfigurator;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.utils.tabular.TableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;

@AutoConfig
public class SimpleAESim implements Runnable {

	private GraphConfigurator fGraphConf;

	private YaoChurnConfigurator fYaoChurn;

	private ExperimentReader fReader;

	@Attribute(value = "antientropy_blacklist")
	private boolean fBlacklist;

	@Attribute(value = "antientropy_shortcycle")
	private double fShortPeriod;

	@Attribute(value = "antientropy_longcycle")
	private double fLongPeriod;

	@Attribute(value = "antientropy_shortcount")
	private int fShortRounds;

	@Attribute(value = "burnin")
	private double fBurnin;

	@Attribute(value = "sim_duration")
	private double fSimulationTime;

	@Attribute(value = "n", defaultValue = "2147483647")
	private int fN;

	public SimpleAESim(@Attribute(Attribute.AUTO) IResolver resolver) {
		fYaoChurn = ObjectCreator.createInstance(YaoChurnConfigurator.class,
				"", resolver);
		fGraphConf = ObjectCreator.createInstance(GraphConfigurator.class, "",
				resolver);
		fReader = ObjectCreator.createInstance(ExperimentReader.class, "",
				resolver);
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void run0() throws Exception {
		IGraphProvider provider = fGraphConf.graphProvider();
		Iterator<Experiment> it = fReader.iterator(provider);
		TableWriter stats = new TableWriter(System.out, "id", "source",
				"target", "send", "reply", "time");

		while (it.hasNext() && fN > 0) {
			Experiment exp = it.next();
			runExperiment(exp, provider, stats);
			fN--;
		}
	}

	private void runExperiment(Experiment exp, IGraphProvider provider,
			TableWriter stats) throws Exception {

		IndexedNeighborGraph graph = provider.subgraph(exp.root);
		int[] ids = provider.verticesOf(exp.root);

		IProcess[] processes = fYaoChurn.createProcesses(exp.lis, exp.dis,
				graph.size());

		Antientropy[] protocols = new Antientropy[processes.length];

		EngineBuilder builder = new EngineBuilder();
		IReference<ISimulationEngine> engineRef = builder.reference();

		Random random = new Random();
		for (int i = 0; i < processes.length; i++) {
			protocols[i] = new Antientropy(engineRef, random, graph,
					processes[i].id(), 0, fShortPeriod, fLongPeriod,
					fShortRounds, fBurnin, fBlacklist);

			processes[i].addObserver(protocols[i]);
			processes[i].addProtocol(protocols[i]);
		}

		builder.addProcess(processes);
		builder.preschedule(new SimulationTerminator(fSimulationTime));
		builder.setExtraPermits(1);
		builder.setBurnin(fBurnin);

		// Runs simulation.
		EDSimulationEngine engine = builder.engine();
		engine.run();

		for (int i = 0; i < graph.size(); i++) {
			stats.set("id", exp.root);
			stats.set("source", exp.attributes.get("node"));
			stats.set("target", ids[i]);
			stats.set("send", protocols[i].initiate());
			stats.set("reply", protocols[i].respond());
			stats.set("time", engine.clock().time());

			stats.emmitRow();
		}
	}
}
