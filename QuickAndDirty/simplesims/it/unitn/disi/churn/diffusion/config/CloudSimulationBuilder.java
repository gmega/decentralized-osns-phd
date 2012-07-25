package it.unitn.disi.churn.diffusion.config;

import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.DiffusionWick;
import it.unitn.disi.churn.diffusion.DiffusionWick.PostMM;
import it.unitn.disi.churn.diffusion.HFloodMM;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.RandomSelector;
import it.unitn.disi.churn.diffusion.cloud.CloudAccessor;
import it.unitn.disi.churn.diffusion.cloud.ICloud;
import it.unitn.disi.churn.diffusion.cloud.SimpleCloudImpl;
import it.unitn.disi.churn.diffusion.graph.CachingTransformer;
import it.unitn.disi.churn.diffusion.graph.LiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.INodeMetric;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.simulator.protocol.PausingCyclicProtocolRunner;
import it.unitn.disi.utils.collections.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CloudSimulationBuilder {

	private static final int HFLOOD_PID = 0;

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
			final int source, IProcess[] processes) throws Exception {

		EDSimulationEngine engine = new EDSimulationEngine(processes, fBurnin);
		List<Pair<Integer, ? extends IEventObserver>> observers = new ArrayList<Pair<Integer, ? extends IEventObserver>>();

		PausingCyclicProtocolRunner<HFloodMM> runner = new PausingCyclicProtocolRunner<HFloodMM>(
				engine, SECOND, 1, HFLOOD_PID);
		observers.add(new Pair<Integer, IEventObserver>(1, runner));
		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, runner.networkObserver()));

		final HFloodMM[] prots = create(processes, runner, source);
		SimpleCloudImpl cloud = new SimpleCloudImpl(fGraph.size(), source);
		create(engine, processes, prots, cloud, source);

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();
		metrics.add(cloud.totalAccesses());
		metrics.add(cloud.productiveAccesses());

		addWickOrAnchor(source, prots, engine, cloud, observers, metrics);

		engine.setEventObservers(observers);

		return new Pair<EDSimulationEngine, List<INodeMetric<? extends Object>>>(
				engine, metrics);

	}

	private void addWickOrAnchor(final int source, final HFloodMM[] prots,
			EDSimulationEngine engine, SimpleCloudImpl cloud,
			List<Pair<Integer, ? extends IEventObserver>> observers,
			List<INodeMetric<? extends Object>> metrics) {

		if (fNUPOnly > 0) {
			Anchor anchor = new Anchor();
			observers.add(new Pair<Integer, IEventObserver>(anchor.type(),
					anchor));
			return;
		}

		DiffusionWick wick = new DiffusionWick(source, fNUPBurnin);
		final PostMM poster = wick.new PostMM(prots[source], cloud);
		wick.setPoster(poster);

		observers.add(new Pair<Integer, IEventObserver>(
				IProcess.PROCESS_SCHEDULABLE_TYPE, wick));

		metrics.add(MMMetrics.rdMetric(prots, wick));
		metrics.add(MMMetrics.edMetric(prots, wick));
	}

	private HFloodMM[] create(IProcess[] processes,
			PausingCyclicProtocolRunner<? extends ICyclicProtocol> runner,
			int source) {
		HFloodMM[] protocols = new HFloodMM[fGraph.size()];
		for (int i = 0; i < protocols.length; i++) {
			protocols[i] = new HFloodMM(HFLOOD_PID, fGraph, peerSelector(),
					processes[i],
					new CachingTransformer(new LiveTransformer()), runner,
					i == source);
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
}
