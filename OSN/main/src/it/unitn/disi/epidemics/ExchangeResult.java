package it.unitn.disi.epidemics;

public class ExchangeResult {

	public static final int UNKNOWN = -1;

	public int bytesSent = UNKNOWN;
	public int bytesReceived = UNKNOWN;

	public int messagesSent = UNKNOWN;
	public int messagesReceived = UNKNOWN;

	public ExchangeResult() {

	}

	public ExchangeResult(int bytesSent, int bytesReceived, int messagesSent,
			int messagesReceived) {
		this.bytesSent = bytesSent;
		this.bytesReceived = bytesReceived;
		this.messagesSent = messagesSent;
		this.messagesReceived = messagesReceived;
	}

}
