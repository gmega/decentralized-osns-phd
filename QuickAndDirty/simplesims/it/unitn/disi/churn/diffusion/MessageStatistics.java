package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.cloud.SessionStatistics;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.measure.INodeMetric;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import peersim.extras.am.util.IncrementalStatsFreq;

public class MessageStatistics extends SessionStatistics implements
		IMessageObserver {

	private static final double SECOND = 1.0 / 3600.0;

	private static final int ANTIENTROPY = 0;

	private static final int HFLOOD = 1;

	private static final int COMBINED = 2;

	private static final int SENT = 0;

	private static final int RECEIVED = 1;

	private static final boolean CARDINALITY_CHECK = false;

	private final int[][][] fUpdates;

	private final int[][][] fQuench;

	private final int[][] fAEDigest;

	private final int[][] fUptimeZeroBins;

	private final BandwidthTracker<?>[][] fBdwTracker;

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

		fBdwTracker = new BandwidthTracker[3][size];
		fUptimeZeroBins = new int[3][size];

		fTrackFrequencies = distributions;
	}

	private BandwidthTracker<?> tracker(double base, double uptimeBase,
			boolean distributions) {
		return distributions ? new BandwidthTrackerFreq(base, uptimeBase,
				SECOND) : new BandwidthTrackerStats(base, uptimeBase, SECOND);
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
			fBdwTracker[ANTIENTROPY][sender].at(clock.rawTime())
					.messageReceived();
			fBdwTracker[ANTIENTROPY][receiver].at(clock.rawTime())
					.messageReceived();
			countAntientropy(sender, receiver, message, clock, flags);
		} else {
			fBdwTracker[HFLOOD][sender].at(clock.rawTime()).messageReceived();
			fBdwTracker[HFLOOD][receiver].at(clock.rawTime()).messageReceived();
			count(HFLOOD, sender, receiver, message, clock, flags);
		}

		fBdwTracker[COMBINED][sender].at(clock.rawTime()).messageReceived();
		fBdwTracker[COMBINED][receiver].at(clock.rawTime()).messageReceived();

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
			if (CARDINALITY_CHECK) {
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

	}

	@Override
	public void stopTrackingSession(IClockData clock) {
		if (CARDINALITY_CHECK) {
			if (fSingle.cardinality() != fSize) {
				throw new IllegalSelectorException();
			}
		}

		INetwork network = clock.engine().network();

		for (int i = 0; i < fBdwTracker[ANTIENTROPY].length; i++) {

			fUptimeZeroBins[ANTIENTROPY][i] = uptimeZeroBins(clock,
					network.process(i), fBdwTracker[ANTIENTROPY][i]);
			fUptimeZeroBins[HFLOOD][i] = uptimeZeroBins(clock,
					network.process(i), fBdwTracker[HFLOOD][i]);
			fUptimeZeroBins[COMBINED][i] = uptimeZeroBins(clock,
					network.process(i), fBdwTracker[COMBINED][i]);

			fBdwTracker[ANTIENTROPY][i].at(clock.rawTime()).end();
			fBdwTracker[HFLOOD][i].at(clock.rawTime()).end();
			fBdwTracker[COMBINED][i].at(clock.rawTime()).end();

			// Bunch of sanity checks.

			// Message counts should match.
			int aeMsgs = fBdwTracker[ANTIENTROPY][i].messageCount();
			int hfMsgs = fBdwTracker[HFLOOD][i].messageCount();
			int alMsgs = fBdwTracker[COMBINED][i].messageCount();

			int aeBuckets = ((IncrementalStatsFreq) fBdwTracker[ANTIENTROPY][i]
					.getStats()).getN();
			int hfBuckets = ((IncrementalStatsFreq) fBdwTracker[HFLOOD][i]
					.getStats()).getN();
			int alBuckets = ((IncrementalStatsFreq) fBdwTracker[COMBINED][i]
					.getStats()).getN();

			int eBuckets = (int) Math
					.ceil((clock.rawTime() - lastSessionStart()) * 3600);

			int aeUpBuckets = aeBuckets
					- ((IncrementalStatsFreq) fBdwTracker[ANTIENTROPY][i]
							.getStats()).getFreq(0)
					+ fUptimeZeroBins[ANTIENTROPY][i];
			int hfUpBuckets = hfBuckets
					- ((IncrementalStatsFreq) fBdwTracker[HFLOOD][i].getStats())
							.getFreq(0) + fUptimeZeroBins[HFLOOD][i];
			int alUpBuckets = alBuckets
					- ((IncrementalStatsFreq) fBdwTracker[COMBINED][i]
							.getStats()).getFreq(0)
					+ fUptimeZeroBins[COMBINED][i];

			int upBuckets = (int) Math
					.ceil((network.process(i).uptime(clock) - fBdwTracker[HFLOOD][i]
							.uptimeBase()) * 3600);

			if (aeMsgs != (fUpdates[SENT][ANTIENTROPY][i]
					+ fUpdates[RECEIVED][ANTIENTROPY][i]
					+ fQuench[SENT][ANTIENTROPY][i]
					+ fQuench[RECEIVED][ANTIENTROPY][i] + fAEDigest[SENT][i] + fAEDigest[RECEIVED][i])) {
				throw new IllegalStateException();
			}

			if (hfMsgs != (fUpdates[SENT][HFLOOD][i]
					+ fUpdates[RECEIVED][HFLOOD][i] + fQuench[SENT][HFLOOD][i] + fQuench[RECEIVED][HFLOOD][i])) {
				throw new IllegalStateException();
			}

			if (alMsgs != (hfMsgs + aeMsgs)) {
				throw new IllegalStateException();
			}

			// Time buckets should match.
			int downZeros = ((IncrementalStatsFreq) fBdwTracker[COMBINED][i]
					.getStats()).getFreq(0);
			if (fUptimeZeroBins[COMBINED][i] > downZeros) {
				throw new IllegalStateException(fUptimeZeroBins[COMBINED][i]
						+ " > " + downZeros);
			}

			checkDrift(aeBuckets, eBuckets, "ANTI");
			checkDrift(hfBuckets, eBuckets, "HFLOOD");
			checkDrift(alBuckets, eBuckets, "ALL");

			checkDrift(aeUpBuckets, upBuckets, "ANTI:ALL");
			checkDrift(hfUpBuckets, upBuckets, "HFLOOD:ALL");
			checkDrift(alUpBuckets, upBuckets, "ALL:ALL");
		}

		super.stopTrackingSession(clock);
	}

	private void checkDrift(int b1, int b2, String id) {
		if (b1 != b2) {
			System.out.println("DRIFT:" + b1 + ", " + b2 + " " + id);
		}
	}

	private int uptimeZeroBins(IClockData clock, IProcess process,
			BandwidthTracker<?> tracker) {
		BandwidthTracker<IncrementalStatsFreq> clone = null;
		try {
			clone = (BandwidthTracker<IncrementalStatsFreq>) tracker.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}

		clone.end(process, clock);

		return clone.getStats().getFreq(0);
	}

	@Override
	public void startTrackingSession(IClockData clock) {
		super.startTrackingSession(clock);

		INetwork network = clock.engine().network();

		if (fInit) {
			return;
		}

		fInit = true;

		for (int i = 0; i < fBdwTracker[COMBINED].length; i++) {
			IProcess process = network.process(i);
			fBdwTracker[HFLOOD][i] = tracker(clock.rawTime(),
					process.uptime(clock), fTrackFrequencies);
			fBdwTracker[ANTIENTROPY][i] = tracker(clock.rawTime(),
					process.uptime(clock), fTrackFrequencies);
			fBdwTracker[COMBINED][i] = tracker(clock.rawTime(),
					process.uptime(clock), fTrackFrequencies);
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
				return (IncrementalStatsFreq) fBdwTracker[ANTIENTROPY][i]
						.getStats();
			}
		});

		metrics.add(new INodeMetric<IncrementalStatsFreq>() {
			@Override
			public Object id() {
				return fId + ".bdw.hf";
			}

			@Override
			public IncrementalStatsFreq getMetric(int i) {
				return (IncrementalStatsFreq) fBdwTracker[HFLOOD][i].getStats();
			}
		});

		metrics.add(new INodeMetric<IncrementalStatsFreq>() {
			@Override
			public Object id() {
				return fId + ".bdw.tot";
			}

			@Override
			public IncrementalStatsFreq getMetric(int i) {
				return (IncrementalStatsFreq) fBdwTracker[COMBINED][i]
						.getStats();
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".bdw.ae.upbins";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUptimeZeroBins[ANTIENTROPY][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".bdw.hf.upbins";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUptimeZeroBins[HFLOOD][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".bdw.tot.upbins";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUptimeZeroBins[COMBINED][i];
			}
		});

		return metrics;
	}
}
