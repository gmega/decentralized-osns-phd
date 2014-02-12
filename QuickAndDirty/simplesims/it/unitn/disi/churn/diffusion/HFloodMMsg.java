package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.DisseminationServiceImpl.BroadcastTracker;

public class HFloodMMsg implements IMessage {

	public final double timestamp;

	public final int source;

	public final int sequence;

	private BroadcastTracker fTracker;
	
	public static HFloodMMsg createUpdate(double timestamp, int source, int sequence) {
		return new HFloodMMsg(timestamp, source, sequence);
	}
	
	public static HFloodMMsg createQuench(double timestamp) {
		return new HFloodMMsg(timestamp, -1, -1);
	}

	private HFloodMMsg(double timestamp, int source, int sequence) {
		this.timestamp = timestamp;
		this.source = source;
		this.sequence = sequence;
	}

	public boolean isNUP() {
		return sequence == -1;
	}

	public int source() {
		return source;
	}

	public double timestamp() {
		return timestamp;
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
			buf.append("Quench");
		} else {
			buf.append("Update");
		}

		buf.append(" message ");
		if (!isNUP()) {
			buf.append("from ");
			buf.append(source);
			buf.append(" ");
		}

		buf.append("@ ");
		buf.append(timestamp);
		return buf.toString();
	}

}
