package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.cloud.ICloud;
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
public class DiffusionWick implements IEventObserver {

	private final int fSource;

	private final double fDelay;

	private double[] fSnapshot;

	private Poster fPoster;

	public DiffusionWick(int source) {
		this(source, 0.0);
	}

	public DiffusionWick(int source, double delay) {
		fSource = source;
		fDelay = delay;
	}

	public void setPoster(Poster poster) {
		fPoster = poster;
	}

	public Poster poster() {
		return fPoster;
	}

	public int source() {
		return fSource;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {
		if (engine.clock().time() < fDelay) {
			return;
		}
		IProcess process = (IProcess) schedulable;
		// First login of the source.
		if (process.id() == fSource && process.isUp()) {
			if (fPoster == null) {
				throw new IllegalStateException(
						"Poster has not been initialized.");
			}
			System.err.println("Wick fired.");
			fPoster.post(engine);
			fSnapshot = uptimeSnapshot(engine.network(), engine.clock());
			// We're no longer binding.
			engine.unbound(this);
		}
	}

	public double up(int i) {
		return fSnapshot[i];
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
		return fSnapshot != null;
	}

	public interface Poster {

		public void post(ISimulationEngine engine);

	}

	public class PostSM implements Poster {

		private HFloodSM fSourceProtocol;

		public PostSM(HFloodSM source) {
			fSourceProtocol = source;
		}

		@Override
		public void post(ISimulationEngine engine) {
			fSourceProtocol.markReached(engine.clock());
		}

	}

	public class PostMM implements Poster {

		private HFloodMM fSourceProtocol;

		private ICloud fCloud;

		private Message fMessage;

		public PostMM(HFloodMM source, ICloud cloud) {
			fSourceProtocol = source;
			fCloud = cloud;
		}

		@Override
		public void post(ISimulationEngine engine) {
			fCloud.resetAccessCounters();
			Message update = new Message(engine.clock().rawTime(), fSource);
			fSourceProtocol.post(update, engine);
			fCloud.writeUpdate(fSource, update);
			fMessage = update;
		}

		public Message getMessage() {
			return fMessage;
		}

	}

}
