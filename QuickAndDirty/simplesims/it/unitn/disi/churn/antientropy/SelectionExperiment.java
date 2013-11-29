package it.unitn.disi.churn.antientropy;

import groovy.lang.Binding;
import it.unitn.disi.churn.config.Experiment;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.experiments.config.PeerSelectorBuilder;
import it.unitn.disi.graph.IGraphProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.utils.streams.PrefixedWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.util.BitSet;
import java.util.Random;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;

@AutoConfig
public class SelectionExperiment extends SimpleGraphExperiment {

	private static final BitSet EMPTY_BITSET = new BitSet();

	private static final double SECOND = 1 / 3600.00D;

	private final String fSelector;

	private TableWriter fWriter;

	private int[] fDegrees;

	public SelectionExperiment(
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(value = "sim_duration") double simulationTime,
			@Attribute(value = "n", defaultValue = "2147483647") int n,
			@Attribute(value = "burnin") double burnin,
			@Attribute(value = "inbound_bdw", defaultValue = "1") double inboundBdw,
			@Attribute(value = "outbound_bdw", defaultValue = "1") double outboundBdw,
			@Attribute(value = "selector") String selector) {
		super(resolver, simulationTime, burnin, n);
		fWriter = new TableWriter(new PrefixedWriter("DT:", System.out), "id",
				"source", "target", "select", "selected", "uptime", "degree");
		fSelector = selector;
	}

	@Override
	protected void runExperiment(Experiment exp, IGraphProvider provider)
			throws Exception {
		IndexedNeighborGraph graph = provider.subgraph(exp.root);

		IProcess[] processes;
		if (exp.lis != null) {
			processes = fYaoChurn.createProcesses(exp.lis, exp.dis,
					graph.size());
			System.err.println("-- Using Yao churn (" + fYaoChurn.mode() + ")");
		} else {
			processes = new IProcess[graph.size()];
			for (int i = 0; i < processes.length; i++) {
				processes[i] = new FixedProcess(i,
						it.unitn.disi.simulator.core.IProcess.State.up);
			}
			System.err.println("-- Static simulation.");
		}

		System.err.println("-- Selector is " + fSelector + ".");

		Random random = new Random();
		int source = Integer.parseInt(exp.attributes.get("node"));

		PeerSelectorBuilder psb = new PeerSelectorBuilder(provider, random,
				exp.root);
		Binding binding = new Binding();
		binding.setVariable("assignments", fReader);
		IPeerSelector[] selectors = psb.build(fSelector, binding);

		int[] ids = provider.verticesOf(exp.root);
		SimpleProtocol[] prots = new SimpleProtocol[processes.length];
		for (int i = 0; i < processes.length; i++) {
			prots[i] = new SimpleProtocol(selectors[i], graph);
			processes[i].addProtocol(prots[i]);
		}

		EngineBuilder builder = new EngineBuilder();

		final PausingCyclicProtocolRunner<SimpleProtocol> runner = new PausingCyclicProtocolRunner<SimpleProtocol>(
				builder.reference(), SECOND, 2, 0);

		builder.addProcess(processes);
		builder.addObserver(runner, 2, false, true);
		builder.addObserver(runner.networkObserver(),
				IProcess.PROCESS_SCHEDULABLE_TYPE, false, true);
		builder.setExtraPermits(1);
		builder.setBurnin(fBurnin);
		builder.stopAt(-1, fSimulationTime, true);

		// If we have no churn, have to wakeup the runner ourselves.
		if (exp.lis == null) {
			builder.preschedule(new Schedulable() {

				private static final long serialVersionUID = 1L;

				@Override
				public int type() {
					return -1;
				}

				@Override
				public double time() {
					return 0;
				}

				@Override
				public void scheduled(ISimulationEngine state) {
					runner.wakeUp();
				}

				@Override
				public boolean isExpired() {
					return true;
				}
			});
		}

		final double[] snapshot = new double[graph.size()];
		builder.addBurninAction(new IEventObserver() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isDone() {
				return false;
			}

			@Override
			public void eventPerformed(ISimulationEngine engine,
					Schedulable schedulable, double nextShift) {
				INetwork network = engine.network();
				IClockData clock = engine.clock();
				for (int i = 0; i < snapshot.length; i++) {
					snapshot[i] = network.process(i).uptime(clock);
				}
			}
		});

		EDSimulationEngine engine = builder.engine();
		engine.run();

		for (int i = 0; i < processes.length; i++) {
			fWriter.set("id", exp.root);
			fWriter.set("source", source);
			fWriter.set("target", ids[i]);
			fWriter.set("select", prots[i].sent());
			fWriter.set("selected", prots[i].received());
			fWriter.set("degree", fDegrees == null ? graph.degree(i)
					: fDegrees[i]);
			fWriter.set("uptime", processes[i].uptime(engine.clock())
					- snapshot[i]);
			fWriter.emmitRow();
		}

		fDegrees = null;
	}

	class SimpleProtocol implements ICyclicProtocol {

		private IPeerSelector fSelector;

		private long fReceive;

		private long fSend;

		private IndexedNeighborGraph fGraph;

		public SimpleProtocol(IPeerSelector selector, IndexedNeighborGraph graph) {
			fSelector = selector;
			fGraph = graph;
		}

		@Override
		public void nextCycle(ISimulationEngine state, IProcess process) {
			if (!process.isUp()) {
				return;
			}

			INetwork network = state.network();
			int peer = fSelector.selectPeer(process.id(), fGraph, EMPTY_BITSET,
					state.network());

			// No selection or throttling round.
			if (peer < 0) {
				return;
			}

			fSend++;
			protocol(network.process(peer)).contact();
		}

		private SimpleProtocol protocol(IProcess peer) {
			return (SimpleProtocol) peer.getProtocol(0);
		}

		private void contact() {
			fReceive++;
		}

		public long sent() {
			return fSend;
		}

		public long received() {
			return fReceive;
		}

		@Override
		public State getState() {
			return State.ACTIVE;
		}

	}

}
