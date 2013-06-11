package it.unitn.disi.churn.diffusion;

import java.nio.channels.IllegalSelectorException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import it.unitn.disi.churn.diffusion.cloud.SessionStatistics;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.measure.INodeMetric;

public class MessageStatistics extends SessionStatistics implements
		IMessageObserver {

	private static final int ANTIENTROPY = 0;

	private static final int HFLOOD = 1;

	private static final int SENT = 0;

	private static final int RECEIVED = 1;

	private final int[][][] fUpdates;

	private final int[][][] fQuench;

	private final int[][] fAEOverhead;

	private final int fSize;

	protected BitSet fSingle = new BitSet();

	public MessageStatistics(Object id, int size) {
		super(id);

		fUpdates = new int[2][2][size];
		fQuench = new int[2][2][size];
		fAEOverhead = new int[2][size];
		fSize = size;
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

		if (message == null) {
			fAEOverhead[SENT][sender]++;
			fAEOverhead[RECEIVED][receiver]++;
			return;
		}

		// Antientropy generates no duplicates.
		if ((flags & HFloodSM.DUPLICATE) != 0) {
			return;
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
	}

	public List<INodeMetric<Double>> metrics() {

		List<INodeMetric<Double>> metrics = new ArrayList<INodeMetric<Double>>();

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
				return (double) fUpdates[RECEIVED][ANTIENTROPY][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.init";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fAEOverhead[SENT][i];
			}
		});

		metrics.add(new INodeMetric<Double>() {
			@Override
			public Object id() {
				return fId + ".ae.respond";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fAEOverhead[RECEIVED][i];
			}
		});

		return metrics;
	}

}
