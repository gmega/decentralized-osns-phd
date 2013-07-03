package it.unitn.disi.churn.diffusion;

import java.util.Arrays;

import it.unitn.disi.churn.diffusion.DisseminationServiceImpl.IBroadcastObserver;
import it.unitn.disi.churn.diffusion.cloud.CloudAccessStatistics;
import it.unitn.disi.churn.diffusion.cloud.ICloud;
import it.unitn.disi.churn.diffusion.experiments.config.SimpleMutableMetric;
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
public class DiffusionWick implements IEventObserver, IBroadcastObserver {

	private static final long serialVersionUID = 1L;

	private static final boolean DUMP_DELAY_POINTS = false;

	private final int fSource;

	private final double fDelay;

	private int fMessages;

	private double[] fSnapshot;

	private DisseminationServiceImpl[] fFlood;

	private Poster fPoster;

	private SimpleMutableMetric fEd;

	private SimpleMutableMetric fRd;

	private CloudAccessStatistics fAll;

	private CloudAccessStatistics fUpdate;

	private double fStartTime;

	private boolean fDisseminating;

	private boolean fFirst = true;

	public DiffusionWick(int source, DisseminationServiceImpl[] flood) {
		this("", source, 1, 0.0, flood);
	}

	public DiffusionWick(String mPrefix, int source, int messages,
			double delay, DisseminationServiceImpl[] flood) {
		fSource = source;
		fDelay = delay;
		fMessages = messages;

		fEd = new SimpleMutableMetric(prefix(mPrefix, "ed"), flood.length);
		fRd = new SimpleMutableMetric(prefix(mPrefix, "rd"), flood.length);

		fAll = new CloudAccessStatistics(prefix(mPrefix, "cloud_all"),
				flood.length);
		fUpdate = new CloudAccessStatistics(prefix(mPrefix, "cloud_upd"),
				flood.length);

		fFlood = flood;
	}

	private Object prefix(String mPrefix, String string) {
		if (mPrefix.equals("")) {
			return string;
		}

		return mPrefix + "." + string;
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

		IClockData clock = engine.clock();

		if (clock.time() < fDelay || fDisseminating) {
			return;
		}

		if (fFirst) {
			fFirst = false;
			fAll.startTrackingSession(clock);
		}

		IProcess process = (IProcess) schedulable;
		// First login of the source.
		if (process.id() == fSource && process.isUp()) {
			if (fPoster == null) {
				throw new IllegalStateException(
						"Poster has not been initialized.");
			}

			fDisseminating = true;
			fUpdate.startTrackingSession(clock);
			System.err.println("Wick fired (" + fMessages + ").");
			fPoster.post(engine);
			fSnapshot = uptimeSnapshot(engine.network(), engine.clock());
			fMessages--;
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

	public CloudAccessStatistics allAccesses() {
		return fUpdate;
	}

	public CloudAccessStatistics updates() {
		return fAll;
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
			fSourceProtocol.markReached(fSource, engine.clock(), 0);
		}

	}

	public class PostMM implements Poster {

		private IDisseminationService fSourceProtocol;

		private ICloud fCloud;

		private HFloodMMsg fMessage;

		public PostMM(ICloud cloud) {
			fSourceProtocol = fFlood[fSource];
			fCloud = cloud;
			if (fCloud != null) {
				fCloud.addAccessListener(fAll);
				fCloud.addAccessListener(fUpdate);
			}
		}

		@Override
		public void post(ISimulationEngine engine) {
			HFloodMMsg update = new HFloodMMsg(engine.clock().rawTime(),
					fSource);
			fSourceProtocol.post(update, engine);
			if (fCloud != null) {
				fCloud.writeUpdate(fSource, fSource, update, engine);
				System.err.println("START TIME: " + engine.clock().rawTime());
			}
			fStartTime = engine.clock().rawTime();
			fMessage = update;
		}

		public HFloodMMsg getMessage() {
			return fMessage;
		}

		public ICloud cloud() {
			return fCloud;
		}

	}

	@Override
	public void broadcastStarted(HFloodMMsg message, ISimulationEngine engine) {
	}

	@Override
	public void broadcastDone(HFloodMMsg message, ISimulationEngine engine) {
		
		HFloodSM source = getSM(fSource);
		for (int i = 0; i < fFlood.length; i++) {
			HFloodSM sm = getSM(i);

			double ed = fEd.getMetric(i);
			double edSample = (sm.rawEndToEndDelay() - source
					.rawEndToEndDelay());
			ed = ed + edSample;
			fEd.setValue(ed, i);

			double rd = fRd.getMetric(i);
			double rdSample = (sm.rawReceiverDelay() - fSnapshot[i]);
			rd = rd + rdSample;
			fRd.setValue(rd, i);

			if (DUMP_DELAY_POINTS) {
				System.out.println("VED: " + fSource + " " + i + " " + edSample
						+ " " + rdSample);
			}
		}

		fUpdate.stopTrackingSession(engine.clock());

		fDisseminating = false;
		Arrays.fill(fSnapshot, Double.MIN_VALUE);

		System.out.println("BT: " + (engine.clock().rawTime() - fStartTime));

		// We're no longer binding.
		if (fMessages == 0) {
			engine.unbound(this);
			fAll.stopTrackingSession(engine.clock());
		}
	}

	public HFloodSM getSM(int i) {
		return fFlood[i].get(((PostMM) fPoster).getMessage());
	}

}
