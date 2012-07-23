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
