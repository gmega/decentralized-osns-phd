package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.DiffusionWick.PostMM;
import it.unitn.disi.churn.diffusion.CoreTracker;
import it.unitn.disi.churn.diffusion.DisseminationServiceImpl;
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
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
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

	public static final int AE_PRIORITY = 0;

	public static final int CA_PRIORITY = 2;

	public static final int ACCESSES = 0;

	public static final int PRODUCTIVE = 0;

	private static final double SECOND = 0.0002777777777777778D;

	private final double fBurnin;

	private final double fDelta;

	private final IndexedNeighborGraph fGraph;

	private final Random fRandom;

	private final char fSelectorType;

	private final double fNUPBurnin;

	private final double fNUPAnchor;

	private final boolean fRandomize;

	private final double fLoginGrace;

	private final double fFixedFraction;

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
			Random random, double nupOnly, double loginGrace,
			double fixedFraction, boolean randomize) {
		fDelta = period;
		fGraph = graph;
		fRandom = random;
		fBurnin = burnin;
		fSelectorType = selectorType;
		fNUPBurnin = nupBurnin;
		fNUPAnchor = nupOnly;
		fRandomize = randomize;
		fLoginGrace = loginGrace;
		fFixedFraction = fixedFraction;
	}

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(
			final int source, int messages, int quenchDesync,
			double pushTimeout, double antientropyShortCycle,
			double antientropyLongCycle, int shortCycles,
			boolean dissemination, boolean cloudAssist, boolean baseline,
			boolean trackCores, boolean aeBlacklist, IProcess[] processes) {

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();
		DisseminationServiceImpl[] prots;
		SimpleCloudImpl cloud = null;

		// Builder for engine.
		EngineBuilder builder = new EngineBuilder();
		builder.addProcess(processes);
		builder.setBurnin(fBurnin);

		IReference<ISimulationEngine> reference = builder.reference();

		int pid = 0;
		// 1. Config for dissemination service.
		// 1a. Pausing runner that runs the push protocols.
		PausingCyclicProtocolRunner<DisseminationServiceImpl> runner = null;
		
		// If dissemination is disabled, don't register a runner for the push
		// protocols.
		if (dissemination) {
			runner = new PausingCyclicProtocolRunner<DisseminationServiceImpl>(
					reference, SECOND, 1, pid);
			builder.addObserver(runner, 1, true, true);
			builder.addObserver(runner.networkObserver(),
					IProcess.PROCESS_SCHEDULABLE_TYPE, false, true);
		}

		prots = create(processes, runner, pid, messages, quenchDesync,
				pushTimeout, 
				// Antientropy cycle length is negative if no dissemination. 
				// This causes antientropy to be disabled.
				dissemination ? antientropyShortCycle : -1,
				antientropyLongCycle, shortCycles, aeBlacklist, metrics,
				builder);

		// 1b. Cloud-assistance, if enabled.
		if (cloudAssist) {
			cloud = new SimpleCloudImpl(source);
			create(reference, processes, prots, cloud, source);
		}

		// 1c. Add dissemination wick (guy that posts update after some
		// period of time ellapses).
		addWickAndAnchor(source, messages, "", prots, processes, builder,
				cloud, metrics);

		// 1d. Add CoreTracker if required -- tracks initial connected core,
		// which we need for the two-phase sims.
		if (trackCores) {
			CoreTracker tracker = new CoreTracker(prots[source], fGraph,
					source, pid);
			builder.addObserver(tracker, IProcess.PROCESS_SCHEDULABLE_TYPE,
					false, true);
			metrics.add(tracker);
		}

		pid += 1;

		return new Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>>(
				builder.engine(), metrics);
	}

	private void addWickAndAnchor(final int source, int messages,
			String prefix, final DisseminationServiceImpl[] prots,
			IProcess[] processes, EngineBuilder builder, SimpleCloudImpl cloud,
			List<INodeMetric<? extends Object>> metrics) {

		if (fNUPAnchor > 0) {
			builder.preschedule(new Anchor());
		}

		DiffusionWick wick = new DiffusionWick(prefix, source, messages,
				fNUPBurnin, prots);
		final PostMM poster = wick.new PostMM(cloud);
		wick.setPoster(poster);

		// Adds the diffusion wick as the global message tracker.
		for (DisseminationServiceImpl protocol : prots) {
			protocol.addBroadcastObserver(wick);
		}

		// If the source is a fixed node, we need to add a synthetic login to
		// fire the wick.
		if (processes[source] instanceof FixedProcess) {
			builder.preschedule(new OneShotProcess(processes[source]));
		}

		builder.addObserver(wick, IProcess.PROCESS_SCHEDULABLE_TYPE, true, true);

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

	private DisseminationServiceImpl[] create(IProcess[] processes,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			int pid, int messages, int quenchDesync, double pushTimeout,
			double antientropyShortCycle, double antientropyLongCycle,
			int shortCycles, boolean aeBlacklist,
			List<INodeMetric<? extends Object>> metrics, EngineBuilder builder) {

		DisseminationServiceImpl[] protocols = new DisseminationServiceImpl[fGraph
				.size()];
		MessageStatistics mstats = new MessageStatistics("msg", fGraph.size());

		for (int i = 0; i < protocols.length; i++) {

			protocols[i] = new DisseminationServiceImpl(pid, fRandom, fGraph,
					peerSelector(), processes[i], new CachingTransformer(
							new LiveTransformer()), runner,
					builder.reference(), messages == 1, quenchDesync,
					maxQuenchAge(), pushTimeout, antientropyShortCycle,
					antientropyLongCycle, fBurnin, shortCycles, AE_PRIORITY,
					aeBlacklist);

			processes[i].addProtocol(protocols[i]);

			// If the period is negative, disables antientropy.
			if (antientropyShortCycle > 0) {
				processes[i].addObserver(protocols[i].antientropy());
			}

			protocols[i].addMessageObserver(mstats);
			protocols[i].addBroadcastObserver(mstats);
		}

		metrics.add(mstats.accruedTime());
		metrics.addAll(mstats.metrics());

		return protocols;
	}

	private double maxQuenchAge() {
		if (!fRandomize) {
			return fDelta;
		}
		return (fFixedFraction * fDelta) + 2 * (1 - fFixedFraction) * fDelta;
	}

	private CloudAccessor[] create(IReference<ISimulationEngine> reference,
			IProcess[] process, DisseminationServiceImpl[] dissemination,
			ICloud cloud, int source) {
		CloudAccessor[] accessors = new CloudAccessor[process.length];
		for (int i = 0; i < accessors.length; i++) {
			if (i == source) {
				continue;
			}
			accessors[i] = new CloudAccessor(reference, dissemination[i],
					cloud, fDelta, fBurnin, fLoginGrace, fFixedFraction, i,
					CA_PRIORITY, fRandomize ? fRandom : null);
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
