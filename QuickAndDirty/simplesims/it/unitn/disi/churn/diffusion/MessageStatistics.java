package it.unitn.disi.churn.diffusion;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import peersim.util.IncrementalStats;

import it.unitn.disi.churn.diffusion.cloud.SessionStatistics;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.measure.INodeMetric;

public class MessageStatistics extends SessionStatistics implements
		IMessageObserver {
	
	private static final double SECOND = 1.0/3600.0;

	private static final int ANTIENTROPY = 0;

	private static final int HFLOOD = 1;

	private static final int SENT = 0;

	private static final int RECEIVED = 1;

	private final int[][][] fUpdates;

	private final int[][][] fQuench;

	private final int[][] fAEDigest;

	private final BandwidthTracker[] fAEBdwTracker;
	
	private final BandwidthTracker[] fHFBdwTracker;

	private final int fSize;

	protected BitSet fSingle = new BitSet();

	public MessageStatistics(Object id, int size) {
		super(id);

		fUpdates = new int[2][2][size];
		fQuench = new int[2][2][size];
		fAEDigest = new int[2][size];
		fSize = size;

		fAEBdwTracker = new BandwidthTracker[size];
		fHFBdwTracker = new BandwidthTracker[size];
		for (int i = 0; i < fAEBdwTracker.length; i++) {
			fAEBdwTracker[i] = new BandwidthTracker(SECOND);
			fHFBdwTracker[i] = new BandwidthTracker(SECOND);
		}
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
			countAntientropy(sender, receiver, message, clock, flags);
		} else {
			count(HFLOOD, sender, receiver, message, clock, flags);
		}

	}

	private void countAntientropy(int sender, int receiver, HFloodMMsg message,
			IClockData clock, int flags) {

		fAEBdwTracker[sender].messageReceived(clock.rawTime());
		fAEBdwTracker[receiver].messageReceived(clock.rawTime());
		
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

		fHFBdwTracker[sender].messageReceived(clock.rawTime());
		fHFBdwTracker[receiver].messageReceived(clock.rawTime());
		
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
	public void stopTrackingSession(double time) {
		super.stopTrackingSession(time);
		if (fSingle.cardinality() != fSize) {
			throw new IllegalSelectorException();
		}
		
		for(BandwidthTracker tracker : fAEBdwTracker) {
			tracker.truncate();
		}
	}

	public List<INodeMetric<? extends Object>> metrics() {

		List<INodeMetric<? extends Object>> metrics = new ArrayList<INodeMetric<? extends Object>>();

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".hflood.up";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUpdates[RECEIVED][HFLOOD][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".hflood.nup";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fQuench[RECEIVED][HFLOOD][i];
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

		metrics.add(new INodeMetric<IncrementalStats>() {
			@Override
			public Object id() {
				return fId + ".bdw.ae";
			}

			@Override
			public IncrementalStats getMetric(int i) {
				return fAEBdwTracker[i].getStats();
			}
		});
		
		metrics.add(new INodeMetric<IncrementalStats>() {
			@Override
			public Object id() {
				return fId + ".bdw.hf";
			}

			@Override
			public IncrementalStats getMetric(int i) {
				return fHFBdwTracker[i].getStats();
			}
		});


		return metrics;
	}
}
