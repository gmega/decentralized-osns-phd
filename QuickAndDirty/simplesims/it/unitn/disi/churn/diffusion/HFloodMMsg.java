package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.HFloodMM.BroadcastTracker;

public class HFloodMMsg implements IMessage {

	public final double fTimestamp;

	public final int fSource;

	private BroadcastTracker fTracker;

	public HFloodMMsg(double timestamp, int source) {
		fTimestamp = timestamp;
		fSource = source;
	}

	public boolean isNUP() {
		return fSource == -1;
	}

	public int source() {
		return fSource;
	}

	public double timestamp() {
		return fTimestamp;
	}

	void setTracker(BroadcastTracker tracker) {
		fTracker = tracker;
	}
	
	BroadcastTracker getTracker() {
		return fTracker;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		if (isNUP()) {
			buf.append("NUP");
		} else {
			buf.append("Update");
		}

		buf.append(" message ");
		if (!isNUP()) {
			buf.append("from ");
			buf.append(fSource);
			buf.append(" ");
		}

		buf.append("@ ");
		buf.append(fTimestamp);
		return buf.toString();
	}

}
