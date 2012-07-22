package it.unitn.disi.churn.diffusion;

public class Message {

	public final double fTimestamp;

	public final int fSource;

	public Message(double timestamp, int source) {
		fTimestamp = timestamp;
		fSource = source;
	}

	public boolean isNUP() {
		return fSource == -1;
	}

	public double timestamp() {
		return fTimestamp;
	}

}
