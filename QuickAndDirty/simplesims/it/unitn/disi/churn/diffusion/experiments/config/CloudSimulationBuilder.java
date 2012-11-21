package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.DiffusionWick.PostMM;
import it.unitn.disi.churn.diffusion.HFloodMM;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.MessageStatistics;
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

	public static final int ACCESSES = 0;

	public static final int PRODUCTIVE = 0;

	private static final double SECOND = 0.0002777777777777778D;

	private final double fBurnin;

	private final double fDelay;

	private final IndexedNeighborGraph fGraph;

	private final Random fRandom;

	private final char fSelectorType;

	private final double fNUPBurnin;

	private final double fNUPAnchor;

	private final boolean fRandomize;
	
	private final double fLoginGrace;

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
			Random random, double nupOnly, double loginGrace, boolean randomize) {
		fDelay = period;
		fGraph = graph;
		fRandom = random;
		fBurnin = burnin;
		fSelectorType = selectorType;
		fNUPBurnin = nupBurnin;
		fNUPAnchor = nupOnly;
		fRandomize = randomize;
		fLoginGrace = loginGrace;
	}

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(
			final int source, int messages, boolean dissemination,
			boolean cloudAssist, boolean baseline, IProcess[] processes) {

		int permits = 0;
		permits += dissemination ? 1 : 0;
		permits += baseline ? 1 : 0;
		permits += fNUPAnchor > 0 ? 1 : 0;

		EDSimulationEngine engine = new EDSimulationEngine(processes, fBurnin);

		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();

		HFloodMM[] prots;
		SimpleCloudImpl cloud = null;

		int pid = 0;
		if (dissemination) {
			PausingCyclicProtocolRunner<HFloodMM> runner = new PausingCyclicProtocolRunner<HFloodMM>(
					engine, SECOND, 1, pid);

			observers.add(new Pair<Integer, IEventObserver>(1, runner));
			observers
					.add(new Pair<Integer, IEventObserver>(
							IProcess.PROCESS_SCHEDULABLE_TYPE, runner
									.networkObserver()));

			prots = create(processes, runner, pid, messages, metrics);

			if (cloudAssist) {
				cloud = new SimpleCloudImpl(source);
				create(engine, processes, prots, cloud, source);
			}

			addWickAndAnchor(source, messages, "", prots, processes, engine,
					cloud, observers, metrics);
			pid++;
		}

		if (cloudAssist && baseline) {
			// Note we don't register a runner for the baseline, as it's not
			// supposed to actually run.
			HFloodMM[] baselineProts = create(processes, null, pid, messages,
					metrics);
			cloud = new SimpleCloudImpl(source);
			create(engine, processes, baselineProts, cloud, source);

			addWickAndAnchor(source, messages, "b", baselineProts, processes,
					engine, cloud, observers, metrics);
		}

		engine.setEventObservers(observers);
		engine.setStopPermits(permits);

		return new Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>>(
				engine, metrics);
	}

	private void addWickAndAnchor(final int source, int messages,
			String prefix, final HFloodMM[] prots, IProcess[] processes,
			EDSimulationEngine engine, SimpleCloudImpl cloud,
			List<Pair<Integer, ? extends IEventObserver>> observers,
			List<INodeMetric<? extends Object>> metrics) {

		if (fNUPAnchor > 0) {
			engine.schedule(new Anchor());
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

		if (cloud != null) {
			metrics.add(wick.allAccesses().accesses(AccessType.productive));
			metrics.add(wick.allAccesses().accesses(AccessType.nup));
			metrics.add(wick.allAccesses().accruedTime());

			metrics.add(wick.updates().accesses(AccessType.productive));
			metrics.add(wick.updates().accesses(AccessType.nup));
			metrics.add(wick.updates().accruedTime());
		}
	}

	private HFloodMM[] create(IProcess[] processes,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			int pid, int messages, List<INodeMetric<? extends Object>> metrics) {
		HFloodMM[] protocols = new HFloodMM[fGraph.size()];
		MessageStatistics mstats = new MessageStatistics("msg", fGraph.size());
		for (int i = 0; i < protocols.length; i++) {
			protocols[i] = new HFloodMM(pid, fGraph, peerSelector(),
					processes[i],
					new CachingTransformer(new LiveTransformer()), runner,
					messages == 1);
			processes[i].addProtocol(protocols[i]);
			protocols[i].addMessageObserver(mstats);
			protocols[i].addBroadcastObserver(mstats);
		}

		metrics.add(mstats.accruedTime());
		metrics.add(mstats.noUpdates());
		metrics.add(mstats.updates());

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
					fDelay, fBurnin, fLoginGrace, i, fRandomize ? fRandom : null);
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
	private class Anchor extends Schedulable {

		private static final long serialVersionUID = 6287804680484080985L;

		@Override
		public boolean isExpired() {
			return true;
		}

		@Override
		public void scheduled(ISimulationEngine engine) {
			System.err.println("Anchor dropped at " + engine.clock().rawTime()
					+ ".");
			engine.stop();
		}

		@Override
		public double time() {
			return fBurnin + fBurnin + fNUPAnchor;
		}

		@Override
		public int type() {
			return 5;
		}

	}

	class OneShotProcess extends IProcess {

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
