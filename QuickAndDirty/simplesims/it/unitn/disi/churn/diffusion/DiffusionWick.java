package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.SimulationState;
import it.unitn.disi.simulator.measure.INodeMetric;

/**
 * {@link DiffusionWick} identifies the initial login of the source node and
 * causes it to be reached "out of the blue".
 * 
 * It also maintains the uptime snapshots required for the computation of the
 * receiver delay, which it exports by means of the {@link INodeMetric}
 * interface.
 */
public class DiffusionWick implements ISimulationObserver, INodeMetric<Double> {

	private final int fPid;

	private final HFlood fSource;

	private EDSimulationEngine fEngine;

	private double[] fSnapshot;

	public DiffusionWick(HFlood source, int pid) {
		fSource = source;
		fPid = pid;
	}

	@Override
	public void eventPerformed(SimulationState state, Schedulable schedulable) {
		IProcess process = (IProcess) schedulable;
		// First login of the source.
		if (process.id() == fSource.id() && process.isUp()) {
			fSource.markReached(state.clock());
			fSnapshot = uptimeSnapshot(state.network(), state.clock());
			// We're no longer binding.
			fEngine.unbound(this);
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
	public void simulationStarted(EDSimulationEngine parent) {
		fEngine = parent;
	}

	@Override
	public boolean isDone() {
		return fSource.isReached();
	}

	@Override
	public boolean isBinding() {
		return true;
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
		HFlood protocol = (HFlood) fEngine.process(i).getProtocol(fPid);
		return protocol.rawReceiverDelay() - fSnapshot[i];
	}

}
