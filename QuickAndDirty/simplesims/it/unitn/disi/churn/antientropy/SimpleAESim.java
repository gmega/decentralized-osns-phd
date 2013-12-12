package it.unitn.disi.churn.antientropy;

import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.experiments.config.PeerSelectorBuilder;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

@AutoConfig
public class SimpleAESim extends SimpleGraphExperiment {

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

	@Attribute(value = "selector")
	private String fSelector;

	private TableWriter fWriter;

	public SimpleAESim(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(value = "sim_duration") double simulationTime,
			@Attribute(value = "burnin") double burnin,
			@Attribute(value = "n", defaultValue = "2147483647") int n) {
		super(resolver, simulationTime, burnin, n);
		fWriter = new TableWriter(new PrefixedWriter("DS:", System.out), "id",
				"source", "target", "send", "reply", "uptime", "time");
	}

	protected void runExperiment(Experiment exp, IGraphProvider provider)
			throws Exception {

		IndexedNeighborGraph graph = provider.subgraph(exp.root);
		int[] ids = provider.verticesOf(exp.root);
		Random random = new Random();

		IProcess[] processes = createProcesses(exp, graph);

		Antientropy[] protocols = new Antientropy[processes.length];
		PeerSelectorBuilder selectorBuilder = new PeerSelectorBuilder(provider,
				random, exp.root);
		IPeerSelector[] selectors = selectorBuilder.build(fSelector);

		EngineBuilder builder = new EngineBuilder();
		IReference<ISimulationEngine> engineRef = builder.reference();

		for (int i = 0; i < processes.length; i++) {
			protocols[i] = new Antientropy(engineRef, random, graph,
					processes[i].id(), 0, fShortPeriod, fLongPeriod,
					fShortRounds, fBurnin, fBlacklist, selectors[i]);

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
		
		IClockData clock = engine.clock();

		for (int i = 0; i < graph.size(); i++) {
			fWriter.set("id", exp.root);
			fWriter.set("source", exp.attributes.get("node"));
			fWriter.set("target", ids[i]);
			fWriter.set("send", protocols[i].initiate());
			fWriter.set("reply", protocols[i].respond());
			fWriter.set("uptime", processes[i].uptime(clock));
			fWriter.set("time", clock.time());

			fWriter.emmitRow();
		}
	}
}
