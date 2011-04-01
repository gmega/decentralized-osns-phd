package it.unitn.disi.test.framework;

public abstract class BaseEvent {
	
	protected final Object fPayload;
	public final long time;

	public BaseEvent(Object payload, long time) {
		fPayload = payload;
		this.time = time;
	}

	abstract void dispatch();
}