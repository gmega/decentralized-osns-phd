package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.measure.INodeMetric;

/**
 * {@link DiffusionWick} identifies the initial login of the source node and
 * causes it to be reached "out of the blue".
 * 
 * It also maintains the uptime snapshots required for the computation of the
 * receiver delay, which it exports by means of the {@link INodeMetric}
 * interface.
 */
@Binding
public class DiffusionWick implements IEventObserver, INodeMetric<Double> {

	private final int fPid;

	private final HFlood fSource;

	private double[] fSnapshot;

	private final ISimulationEngine fEngine;

	public DiffusionWick(ISimulationEngine engine, HFlood source, int pid) {
		fSource = source;
		fPid = pid;
		fEngine = engine;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {
		IProcess process = (IProcess) schedulable;
		// First login of the source.
		if (process.id() == fSource.id() && process.isUp()) {
			fSource.markReached(engine.clock());
			fSnapshot = uptimeSnapshot(engine.network(), engine.clock());
			// We're no longer binding.
			engine.unbound(this);
		}
	}

	private double[] uptimeSnapshot(INetwork network, IClockData clock) {
		double[] snapshot = new double[network.size()];
		for (int i = 0; i < snapshot.length; i++) {
			snapshot[i] = network.process(i).uptime(clock);
		}
		return snapshot;
	}

	@Override
	public boolean isDone() {
		return fSource.isReached();
	}

	@Override
	public Object id() {
		return "rd";
	}

	@Override
	public Double getMetric(int i) {
		if (fSnapshot == null) {
			throw new IllegalStateException("Source not yet reached!");
		}
		HFlood protocol = (HFlood) fEngine.network().process(i)
				.getProtocol(fPid);
		return protocol.rawReceiverDelay() - fSnapshot[i];
	}

}
