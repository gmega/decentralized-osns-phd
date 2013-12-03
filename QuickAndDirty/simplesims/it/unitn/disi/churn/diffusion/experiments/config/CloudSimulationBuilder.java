package it.unitn.disi.churn.diffusion.experiments.config;

import it.unitn.disi.churn.diffusion.Anchor;
import it.unitn.disi.churn.diffusion.CoreTracker;
import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.DiffusionWick.PostMM;
import it.unitn.disi.churn.diffusion.DisseminationServiceImpl;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.MessageStatistics;
import it.unitn.disi.churn.diffusion.UptimeTracker;
import it.unitn.disi.churn.diffusion.cloud.CloudAccessor;
import it.unitn.disi.churn.diffusion.cloud.ICloud;
import it.unitn.disi.churn.diffusion.cloud.ICloud.AccessType;
import it.unitn.disi.churn.diffusion.cloud.ITimeWindowTracker;
import it.unitn.disi.churn.diffusion.cloud.SimpleCloudImpl;
import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.RenewalProcess;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.collections.Triplet;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

public class CloudSimulationBuilder {

	public static final int AE_PRIORITY = 0;

	public static final int CA_PRIORITY = 2;

	public static final int ACCESSES = 0;

	public static final int PRODUCTIVE = 0;

	private static final double SECOND = 0.0002777777777777778D;

	private static final int ANCHOR_TYPE = 5;

	private final double fBurnin;

	private final double fDelta;

	private final IndexedNeighborGraph fGraph;

	private final Random fRandom;

	private final double fNUPBurnin;

	private final double fNUPAnchor;

	private final boolean fRandomize;

	private final double fLoginGrace;

	private final double fFixedFraction;

	private IPeerSelector[] fUpdateSelectors;

	private IPeerSelector[] fQuenchSelectors;

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
			double nupBurnin, IndexedNeighborGraph graph, Random random,
			double nupOnly, double loginGrace, double fixedFraction,
			boolean randomize, IPeerSelector[] updateSelectors,
			IPeerSelector[] quenchSelectors) {
		fDelta = period;
		fGraph = graph;
		fRandom = random;
		fBurnin = burnin;
		fNUPBurnin = nupBurnin;
		fNUPAnchor = nupOnly;
		fRandomize = randomize;
		fLoginGrace = loginGrace;
		fFixedFraction = fixedFraction;
		fUpdateSelectors = updateSelectors;
		fQuenchSelectors = quenchSelectors;
	}

	public Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>> build(
			final int source, int messages, double pushTimeout,
			double antientropyShortCycle, double antientropyLongCycle,
			double antientropyLAThreshold, int shortCycles,
			boolean dissemination, boolean cloudAssist, boolean trackCores,
			boolean aeBlacklist, IProcess[] processes) {

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

		prots = create(processes, runner, source,
				pid,
				messages,
				pushTimeout,
				// Antientropy cycle length is negative if no dissemination.
				// This causes antientropy to be disabled.
				dissemination ? antientropyShortCycle : -1,
				antientropyLongCycle, shortCycles, antientropyLAThreshold,
				aeBlacklist, builder);

		// 1b. Cloud-assistance, if enabled.
		if (cloudAssist) {
			cloud = new SimpleCloudImpl(source);
			create(reference, processes, prots, cloud, source);
		}

		// If no dissemination is being simulated, we turn off message tracking,
		// as it can be quite expensive even if it's doing nothing.
		MessageStatistics mstats = null;
		if (dissemination) {
			mstats = new MessageStatistics("msg", fGraph.size(), true);
			metrics.add(mstats.accruedTime());
			metrics.addAll(mstats.metrics());
		}

		UptimeTracker upTracker = new UptimeTracker("msg", processes.length);
		metrics.add(upTracker.accruedUptime());

		// 1c. Add dissemination wick (guy that posts update after some
		// period of time ellapses), or an anchor for updateless sims. These
		// essentially determine the measurement window for the sims (interval
		// between events along which we measure stuff) and contain the
		// mechanisms that stop the simulation once its done.
		if (fNUPAnchor < 0) {
			addWick(source, messages, "", prots, processes, builder, cloud,
					metrics, mstats, upTracker);
		} else {
			addAnchor(prots, builder, cloud, metrics, mstats, upTracker);
		}

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

	private void addAnchor(DisseminationServiceImpl[] protocols,
			EngineBuilder builder, SimpleCloudImpl cloud,
			List<INodeMetric<? extends Object>> metrics,
			MessageStatistics mstats, final UptimeTracker tracker) {
		Anchor anchor = new Anchor(ANCHOR_TYPE, protocols.length, fBurnin,
				fNUPAnchor, cloud);

		metrics.add(anchor.statistics().accesses(AccessType.nup));
		metrics.add(anchor.statistics().accesses(AccessType.productive));
		metrics.add(anchor.statistics().accruedTime());

		anchor.addMeasurementSessionObserver(new ITimeWindowTracker() {
			@Override
			public void startTrackingSession(IClockData clock) {
				tracker.broadcastStarted(null, clock.engine());
			}

			@Override
			public void stopTrackingSession(IClockData clock) {
				tracker.broadcastDone(null, clock.engine());
			}
		});

		builder.preschedule(anchor);

		if (mstats != null) {
			anchor.addMeasurementSessionObserver(mstats);
			for (int i = 0; i < protocols.length; i++) {
				protocols[i].addMessageObserver(mstats);
			}
		} else {
			builder.setExtraPermits(1);
		}
	}

	private void addWick(final int source, int messages, String prefix,
			final DisseminationServiceImpl[] protocols, IProcess[] processes,
			EngineBuilder builder, SimpleCloudImpl cloud,
			List<INodeMetric<? extends Object>> metrics,
			MessageStatistics mstats, UptimeTracker tracker) {

		DiffusionWick wick = new DiffusionWick(prefix, source, messages,
				fNUPBurnin, protocols);
		final PostMM poster = wick.new PostMM(cloud);
		wick.setPoster(poster);

		// Adds the diffusion wick as the global message tracker.
		for (DisseminationServiceImpl protocol : protocols) {
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

		for (int i = 0; i < protocols.length; i++) {
			protocols[i].addBroadcastObserver(tracker);
			if (mstats != null) {
				protocols[i].addBroadcastObserver(mstats);
				protocols[i].addMessageObserver(mstats);
			}
		}
	}

	private DisseminationServiceImpl[] create(IProcess[] processes,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			int source, int pid, int messages, double pushTimeout,
			double antientropyShortCycle, double antientropyLongCycle,
			int shortCycles, double antientropyLAThreshold,
			boolean aeBlacklist, EngineBuilder builder) {

		final DisseminationServiceImpl[] protocols = new DisseminationServiceImpl[fGraph
				.size()];

		for (int i = 0; i < protocols.length; i++) {

			// Does not blacklist LA node neighbors to avoid getting into
			// selection avoidance deadlocks.
			boolean isLA = ((RenewalProcess) processes[i])
					.asymptoticAvailability() < antientropyLAThreshold;

			protocols[i] = new DisseminationServiceImpl(
					pid,
					fRandom,
					fGraph,
					fUpdateSelectors == null ? null : fUpdateSelectors[i],
					fQuenchSelectors == null ? null : fQuenchSelectors[i],
					processes[i],
					new ILiveTransformer() {
						@Override
						public Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> live(
								IndexedNeighborGraph source, INetwork network) {
							return null;
						}
					}, runner, builder.reference(), messages == 1,
					maxQuenchAge(), pushTimeout, antientropyShortCycle,
					antientropyLongCycle, fBurnin, isLA ? new BitSet()
							: availabilityBlacklist(processes,
									antientropyLAThreshold), shortCycles,
					AE_PRIORITY, aeBlacklist);

			processes[i].addProtocol(protocols[i]);

			// If the period is negative, disables antientropy.
			if (antientropyShortCycle > 0) {
				processes[i].addObserver(protocols[i].antientropy());
			}

		}

		return protocols;
	}

	private BitSet availabilityBlacklist(IProcess[] process, double threshold) {
		BitSet blacklist = new BitSet();
		for (int i = 0; i < process.length; i++) {
			RenewalProcess rp = (RenewalProcess) process[i];
			blacklist.set(i, rp.asymptoticAvailability() < threshold);
		}

		return blacklist;
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

		@Override
		public double asymptoticAvailability() {
			return 1.0;
		}

	}
}
