package it.unitn.disi.network.churn.tracebased;

public class TraceEvent {
	enum EventType {
		UP, DOWN;
	}
	
	public final EventType type;
	public final double time;
	public final String nodeId;

	public TraceEvent(EventType type, double time, String nodeId) {
		this.type = type;
		this.time = time;
		this.nodeId = nodeId;
	}
	
	public String toString() {
		return this.time + " " + this.nodeId + " " + this.type;
	}
}
