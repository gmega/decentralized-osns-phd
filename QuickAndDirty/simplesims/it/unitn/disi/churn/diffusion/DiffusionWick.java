package it.unitn.disi.churn.diffusion;

import java.util.Arrays;

import it.unitn.disi.churn.diffusion.HFloodMM.IBroadcastObserver;
import it.unitn.disi.churn.diffusion.cloud.ICloud;
import it.unitn.disi.churn.diffusion.experiments.config.SimpleMutableMetric;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.measure.INodeMetric;

/**
 * {@link DiffusionWick} starts the update dissemination from the source and
 * collects the metrics for each message.
 * 
 * It also maintains the uptime snapshots required for the computation of the
 * receiver delay, which it exports by means of the {@link INodeMetric}
 * interface.
 */
@Binding
public class DiffusionWick implements IEventObserver, IBroadcastObserver {

	private final int fSource;

	private final double fDelay;

	private int fMessages;

	private double[] fSnapshot;

	private HFloodMM[] fFlood;

	private Poster fPoster;

	private SimpleMutableMetric fEd;

	private SimpleMutableMetric fRd;
	
	private boolean fDisseminating;

	public DiffusionWick(int source, HFloodMM[] flood) {
		this(source, 1, 0.0, flood);
	}

	public DiffusionWick(int source, int messages, double delay,
			HFloodMM[] flood) {
		fSource = source;
		fDelay = delay;
		fMessages = messages;

		fEd = new SimpleMutableMetric("ed", flood.length);
		fRd = new SimpleMutableMetric("rd", flood.length);

		fFlood = flood;
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
		if (engine.clock().time() < fDelay || fDisseminating) {
			return;
		}

		IProcess process = (IProcess) schedulable;
		// First login of the source.
		if (process.id() == fSource && process.isUp()) {
			if (fPoster == null) {
				throw new IllegalStateException(
						"Poster has not been initialized.");
			}
			
			fDisseminating = true;
			
			System.err.println("Wick fired (" + fMessages + ").");
			fPoster.post(engine);
			fSnapshot = uptimeSnapshot(engine.network(), engine.clock());
			fMessages--;

			// We're no longer binding.
			if (fMessages == 0) {
				engine.unbound(this);
				engine.stop();
			}
			
		}
	}

	public double up(int i) {
		return fSnapshot[i];
	}
	
	public INodeMetric<Double> ed() {
		return fEd;
	}
	
	public INodeMetric<Double> rd() {
		return fRd;
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
		return fMessages == 0;
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

		private IDisseminationService fSourceProtocol;

		private ICloud fCloud;

		private HFloodMMsg fMessage;

		public PostMM(ICloud cloud) {
			fSourceProtocol = fFlood[fSource];
			fCloud = cloud;
		}

		@Override
		public void post(ISimulationEngine engine) {
			fCloud.resetAccessCounters();
			HFloodMMsg update = new HFloodMMsg(engine.clock().rawTime(), fSource);
			fSourceProtocol.post(update, engine);
			fCloud.writeUpdate(fSource, update);
			System.err.println("START TIME: " + engine.clock().rawTime());
			fMessage = update;
		}

		public HFloodMMsg getMessage() {
			return fMessage;
		}

	}

	@Override
	public void broadcastDone(HFloodMMsg message) {
		HFloodSM source = getSM(fSource);
		for (int i = 0; i < fFlood.length; i++) {
			HFloodSM sm = getSM(i);
			
			double ed = fEd.getMetric(i);
			ed = ed + (sm.rawEndToEndDelay() - source.rawEndToEndDelay()); 
			fEd.setValue(ed, i);
			
			double rd = fRd.getMetric(i);
			rd = rd + (sm.rawReceiverDelay() - fSnapshot[i]);
			fRd.setValue(rd, i);
		}
		
		fDisseminating = false;
		Arrays.fill(fSnapshot, Double.MIN_VALUE);
	}

	public HFloodSM getSM(int i) {
		return fFlood[i].get(((PostMM) fPoster).getMessage());
	}

}
