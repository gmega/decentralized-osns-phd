package it.unitn.disi.churn.diffusion;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import peersim.extras.am.util.IncrementalStatsFreq;

import it.unitn.disi.churn.diffusion.cloud.SessionStatistics;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.measure.INodeMetric;

public class MessageStatistics extends SessionStatistics implements
		IMessageObserver {

	private static final boolean DEBUG = false;

	private static final double SECOND = 1.0 / 3600.0;

	private static final int ANTIENTROPY = 0;

	private static final int HFLOOD = 1;

	private static final int SENT = 0;

	private static final int RECEIVED = 1;

	private final int[][][] fUpdates;

	private final int[][][] fQuench;

	private final int[][] fAEDigest;

	private final BandwidthTracker<?>[] fAEBdwTracker;

	private final BandwidthTracker<?>[] fHFBdwTracker;

	private boolean fInit;

	private boolean fTrackFrequencies;

	private final int fSize;

	protected BitSet fSingle = new BitSet();

	public MessageStatistics(Object id, int size, boolean distributions) {
		super(id);

		fUpdates = new int[2][2][size];
		fQuench = new int[2][2][size];
		fAEDigest = new int[2][size];
		fSize = size;

		fAEBdwTracker = new BandwidthTracker[size];
		fHFBdwTracker = new BandwidthTracker[size];
		fTrackFrequencies = distributions;
	}

	private BandwidthTracker<?> tracker(double base, boolean distributions) {
		return distributions ? new BandwidthTrackerFreq(base, SECOND, DEBUG)
				: new BandwidthTrackerStats(base, SECOND, DEBUG);
	}

	@Override
	public void messageReceived(int sender, int receiver, HFloodMMsg message,
			IClockData clock, int flags) {

		// Doesn't count outside of sessions.
		if (!isCounting()) {
			return;
		}

		if ((flags & HFloodSM.ANTIENTROPY_PULL) != 0
				|| (flags & HFloodSM.ANTIENTROPY_PUSH) != 0) {
			fAEBdwTracker[sender].at(clock.rawTime()).messageReceived();
			fAEBdwTracker[receiver].at(clock.rawTime()).messageReceived();
			countAntientropy(sender, receiver, message, clock, flags);
		} else {
			fHFBdwTracker[sender].at(clock.rawTime()).messageReceived();
			fHFBdwTracker[receiver].at(clock.rawTime()).messageReceived();
			count(HFLOOD, sender, receiver, message, clock, flags);
		}

	}

	private void countAntientropy(int sender, int receiver, HFloodMMsg message,
			IClockData clock, int flags) {

		if (message == null) {
			fAEDigest[SENT][sender]++;
			fAEDigest[RECEIVED][receiver]++;
			return;
		}

		// Antientropy generates no duplicates.
		if ((flags & HFloodSM.DUPLICATE) != 0) {
			throw new IllegalStateException();
		}

		count(ANTIENTROPY, sender, receiver, message, clock, flags);
	}

	private void count(int protocol, int sender, int receiver,
			HFloodMMsg message, IClockData clock, int flags) {

		if (message.isNUP()) {
			fQuench[SENT][protocol][sender]++;
			fQuench[RECEIVED][protocol][receiver]++;
		} else {
			fUpdates[SENT][protocol][sender]++;
			fUpdates[RECEIVED][protocol][receiver]++;

			if ((flags & HFloodSM.DUPLICATE) == 0) {
				if (fSingle.get(receiver)) {
					throw new IllegalStateException();
				}
				fSingle.set(receiver);
			} else {
				if (!fSingle.get(receiver)) {
					throw new IllegalStateException();
				}
			}
		}

	}

	@Override
	public void stopTrackingSession(IClockData clock) {
		if (fSingle.cardinality() != fSize) {
			throw new IllegalSelectorException();
		}

		System.out.println("TRUN: " + clock.rawTime());

		for (int i = 0; i < fAEBdwTracker.length; i++) {
			fAEBdwTracker[i].at(clock.rawTime()).end();
			fHFBdwTracker[i].at(clock.rawTime()).end();

			// Bunch of sanity checks.
			int aeMsgs = fAEBdwTracker[i].messageCount();
			int hfMsgs = fHFBdwTracker[i].messageCount();

			int aeBuckets = ((IncrementalStatsFreq) fAEBdwTracker[i].getStats())
					.getN();
			int hfBuckets = ((IncrementalStatsFreq) fHFBdwTracker[i].getStats())
					.getN();

			int eBuckets = (int) Math
					.ceil((clock.rawTime() - lastSessionStart()) * 3600);

			if (aeMsgs != (fUpdates[SENT][ANTIENTROPY][i]
					+ fUpdates[RECEIVED][ANTIENTROPY][i]
					+ fQuench[SENT][ANTIENTROPY][i]
					+ fQuench[RECEIVED][ANTIENTROPY][i] + fAEDigest[SENT][i] + fAEDigest[RECEIVED][i])) {
				throw new IllegalSelectorException();
			}

			if (hfMsgs != (fUpdates[SENT][HFLOOD][i]
					+ fUpdates[RECEIVED][HFLOOD][i] + fQuench[SENT][HFLOOD][i] + fQuench[RECEIVED][HFLOOD][i])) {
				throw new IllegalSelectorException();
			}

			if (Math.abs(aeBuckets - eBuckets) > 1) {
				throw new IllegalStateException(i + ": " + aeBuckets + " != "
						+ eBuckets);
			}

			if (Math.abs(hfBuckets - eBuckets) > 1) {
				throw new IllegalStateException(i + ": " + hfBuckets + " != "
						+ eBuckets);
			}

		}

		super.stopTrackingSession(clock);
	}

	@Override
	public void startTrackingSession(IClockData data) {
		super.startTrackingSession(data);

		if (fInit) {
			return;
		}
		fInit = true;

		for (int i = 0; i < fAEBdwTracker.length; i++) {
			fAEBdwTracker[i] = tracker(data.rawTime(), fTrackFrequencies);
			fHFBdwTracker[i] = tracker(data.rawTime(), fTrackFrequencies);
		}
	}

	public List<INodeMetric<? extends Object>> metrics() {

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".hflood.rec.up";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUpdates[RECEIVED][HFLOOD][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".hflood.rec.nup";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fQuench[RECEIVED][HFLOOD][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".hflood.sent.up";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUpdates[SENT][HFLOOD][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".hflood.sent.nup";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fQuench[SENT][HFLOOD][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.sent.up";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUpdates[SENT][ANTIENTROPY][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.sent.nup";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fQuench[SENT][ANTIENTROPY][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.rec.up";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUpdates[RECEIVED][ANTIENTROPY][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.rec.nup";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fQuench[RECEIVED][ANTIENTROPY][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.init";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fAEDigest[SENT][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.respond";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fAEDigest[RECEIVED][i];
			}
		});

		metrics.add(new INodeMetric<IncrementalStatsFreq>() {
			@Override
			public Object id() {
				return fId + ".bdw.ae";
			}

			@Override
			public IncrementalStatsFreq getMetric(int i) {
				return (IncrementalStatsFreq) fAEBdwTracker[i].getStats();
			}
		});

		metrics.add(new INodeMetric<IncrementalStatsFreq>() {
			@Override
			public Object id() {
				return fId + ".bdw.hf";
			}

			@Override
			public IncrementalStatsFreq getMetric(int i) {
				return (IncrementalStatsFreq) fHFBdwTracker[i].getStats();
			}
		});

		return metrics;
	}
}
