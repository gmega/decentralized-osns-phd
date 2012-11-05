package it.unitn.disi.churn.diffusion;

import java.nio.channels.IllegalSelectorException;
import java.util.BitSet;

import it.unitn.disi.churn.diffusion.cloud.SessionStatistics;
import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.measure.INodeMetric;

public class MessageStatistics extends SessionStatistics implements
		IMessageObserver {

	private final int[] fUpdatesReceived;

	private final int[] fNUpsReceived;
	
	protected BitSet fSingle = new BitSet();

	public MessageStatistics(Object id, int size) {
		super(id);

		fUpdatesReceived = new int[size];
		fNUpsReceived = new int[size];
	}

	@Override
	public void messageReceived(IProcess process, HFloodMMsg message,
			IClockData clock, boolean duplicate) {

		// Doesn't count outside of sessions.
		if (!isCounting()) {
			return;
		}

		if (message.isNUP()) {
			fNUpsReceived[process.id()]++;
		} else {
			if (!duplicate) {
				if (fSingle.get(process.id())) {
					throw new IllegalStateException();
				}
			}
			
			fSingle.set(process.id());
			fUpdatesReceived[process.id()]++;
		}

	}
	
	@Override
	public void stopTrackingSession(double time) {
		super.stopTrackingSession(time);
		if (fSingle.cardinality() != fUpdatesReceived.length) {
			throw new IllegalSelectorException();
		}
	}
	
	public INodeMetric<Double> updates() {
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return fId + ".up";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fUpdatesReceived[i];
			}

		};
	}
	
	public INodeMetric<Double> noUpdates() {
		return new INodeMetric<Double>() {

			@Override
			public Object id() {
				return fId + ".nup";
			}

			@Override
			public Double getMetric(int i) {
				return (double) fNUpsReceived[i];
			}

		};
	}

}
