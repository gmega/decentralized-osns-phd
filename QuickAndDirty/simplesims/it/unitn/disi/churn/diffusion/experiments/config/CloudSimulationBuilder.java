package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.DiffusionWick.PostMM;
import it.unitn.disi.churn.diffusion.HFloodMM;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.RandomSelector;
import it.unitn.disi.churn.diffusion.cloud.CloudAccessor;
import it.unitn.disi.churn.diffusion.cloud.ICloud;
import it.unitn.disi.churn.diffusion.cloud.ICloud.AccessType;
import it.unitn.disi.churn.diffusion.cloud.SimpleCloudImpl;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.utils.collections.Pair;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CloudSimulationBuilder {

	private static final int HFLOOD_PID = 0;

	private static final int BASELINE_PID = 1;

	public static final int ACCESSES = 0;

	public static final int PRODUCTIVE = 0;

	private static final double SECOND = 0.0002777777777777778D;

	private final double fBurnin;

	private final double fDelay;

	private final IndexedNeighborGraph fGraph;

	private final Random fRandom;

	private final char fSelectorType;

	private final double fNUPBurnin;

	private final double fNUPOnly;

	/**
	 * Creates a new {@link CloudSimulationBuilder}.
	 * 
	 * @param burnin
	 *            burn-in period with which to run the simulations.
	 * @param period
	 *            cloud access period: nodes will access the cloud whenever they
	 *            go without news for more than "period" time instants.
	 * @param selectorType
	 *            the selector type for the dissemination protocol.
	 * @param graph
	 *            graph over which to disseminate.
	 * @param nupOnly
	 *            if this is set to a non-negative value, simulations will not
	 *            generate updates. Instead, they will run for nupOnly hours
	 *            (simulation time) without updates.
	 * @param random
	 *            random number generator.
	 */
	public CloudSimulationBuilder(double burnin, double period,
			double nupBurnin, char selectorType, IndexedNeighborGraph graph,
			Random random, double nupOnly) {
		fDelay = period;
		fGraph = graph;
		fRandom = random;
		fBurnin = burnin;
		fSelectorType = selectorType;
		fNUPBurnin = nupBurnin;
		fNUPOnly = nupOnly;
	}

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(
			final int source, int messages, boolean baseline,
			IProcess[] processes) {

		EDSimulationEngine engine = new EDSimulationEngine(processes, fBurnin,
				baseline ? 2 : 1);
		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();

		PausingCyclicProtocolRunner<HFloodMM> runner = new Runner(engine,
				SECOND, 1, HFLOOD_PID);
		observers.add(new Pair<Integer, IEventObserver>(1, runner));
		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, runner.networkObserver()));

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();

		HFloodMM[] prots = create(processes, runner, HFLOOD_PID, messages,
				false);
		SimpleCloudImpl cloud = new SimpleCloudImpl(source);
		create(engine, processes, prots, cloud, source);

		addWickOrAnchor(source, messages, "", prots, processes, engine, cloud,
				observers, metrics);

		if (baseline) {
			HFloodMM[] baselineProts = create(processes, runner, BASELINE_PID,
					messages, true);
			cloud = new SimpleCloudImpl(source);
			create(engine, processes, baselineProts, cloud, source);

			addWickOrAnchor(source, messages, "b", baselineProts, processes,
					engine, cloud, observers, metrics);
		}

		engine.setEventObservers(observers);

		return new Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>>(
				engine, metrics);
	}

	private void addWickOrAnchor(final int source, int messages, String prefix,
			final HFloodMM[] prots, IProcess[] processes,
			EDSimulationEngine engine, SimpleCloudImpl cloud,
			List<Pair<Integer, ? extends IEventObserver>> observers,
			List<INodeMetric<? extends Object>> metrics) {

		if (fNUPOnly > 0) {
			Anchor anchor = new Anchor();
			observers.add(new Pair<Integer, IEventObserver>(anchor.type(),
					anchor));
			return;
		}

		DiffusionWick wick = new DiffusionWick(prefix, source, messages,
				fNUPBurnin, prots);
		final PostMM poster = wick.new PostMM(cloud);
		wick.setPoster(poster);

		// Adds the diffusion wick as the global message tracker.
		for (HFloodMM protocol : prots) {
			protocol.addBroadcastObserver(wick);
		}

		// If the source is a fixed node, we need to add a synthetic login to
		// fire the wick.
		if (processes[source] instanceof FixedProcess) {
			engine.schedule(new OneShotProcess(processes[source]));
		}

		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, wick));

		metrics.add(wick.ed());
		metrics.add(wick.rd());

		metrics.add(wick.allAccesses().accesses(AccessType.productive));
		metrics.add(wick.allAccesses().accesses(AccessType.nup));
		metrics.add(wick.allAccesses().accruedTime());

		metrics.add(wick.updates().accesses(AccessType.productive));
		metrics.add(wick.updates().accesses(AccessType.nup));
		metrics.add(wick.updates().accruedTime());
	}

	private HFloodMM[] create(IProcess[] processes,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			int pid, int messages, boolean baseline) {
		HFloodMM[] protocols = new HFloodMM[fGraph.size()];
		for (int i = 0; i < protocols.length; i++) {
			protocols[i] = new HFloodMM(pid, fGraph, peerSelector(),
					processes[i],
					new CachingTransformer(new LiveTransformer()), runner,
					baseline, messages == 1);
			processes[i].addProtocol(protocols[i]);
		}
		return protocols;
	}

	private CloudAccessor[] create(EDSimulationEngine engine,
			IProcess[] process, HFloodMM[] dissemination, ICloud cloud,
			int source) {
		CloudAccessor[] accessors = new CloudAccessor[process.length];
		for (int i = 0; i < accessors.length; i++) {
			if (i == source) {
				continue;
			}
			accessors[i] = new CloudAccessor(engine, dissemination[i], cloud,
					fDelay, fBurnin, i);
			process[i].addObserver(accessors[i]);
			dissemination[i].addMessageObserver(accessors[i]);
		}
		return accessors;
	}

	protected IPeerSelector peerSelector() {
		switch (fSelectorType) {
		case 'a':
			return new BiasedCentralitySelector(fRandom, true);
		case 'r':
			return new RandomSelector(fRandom);
		case 'c':
			return new BiasedCentralitySelector(fRandom, false);
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Binding
	private class Anchor extends Schedulable implements IEventObserver {

		private static final long serialVersionUID = 6287804680484080985L;

		private boolean fDone = false;

		@Override
		public void eventPerformed(ISimulationEngine engine,
				Schedulable schedulable, double nextShift) {
			engine.unbound(this);
			fDone = true;
		}

		@Override
		public boolean isDone() {
			return fDone;
		}

		@Override
		public boolean isExpired() {
			return fDone;
		}

		@Override
		public void scheduled(ISimulationEngine state) {
			// Nothing to do.
		}

		@Override
		public double time() {
			return fNUPOnly;
		}

		@Override
		public int type() {
			return 5;
		}

	}

	@Binding
	private class Runner extends PausingCyclicProtocolRunner<HFloodMM> {

		private static final long serialVersionUID = -1967306468271406257L;

		public Runner(ISimulationEngine engine, double period, int type, int pid) {
			super(engine, period, type, pid);
		}

		// @Override
		// protected boolean hasReachedEndState(ISimulationEngine engine,
		// ICyclicProtocol protocol) {
		// return ((HFloodMM) protocol).isReached();
		// }
	}

	private class OneShotProcess extends IProcess {

		private static final long serialVersionUID = 9180863943276733681L;

		private final IProcess fDelegate;

		private boolean fExpired;

		public OneShotProcess(IProcess delegate) {
			fDelegate = delegate;
		}

		@Override
		public double uptime(IClockData clock) {
			return fDelegate.uptime(clock);
		}

		@Override
		public boolean isUp() {
			return fDelegate.isUp();
		}

		@Override
		public State state() {
			return fDelegate.state();
		}

		@Override
		public int id() {
			return fDelegate.id();
		}

		@Override
		public boolean isExpired() {
			return fExpired;
		}

		@Override
		public void scheduled(ISimulationEngine state) {
			if (fExpired) {
				throw new IllegalSelectorException();
			}
			fExpired = true;
		}

		@Override
		public double time() {
			return fBurnin + fNUPBurnin + 0.0000000001D;
		}

	}
}
