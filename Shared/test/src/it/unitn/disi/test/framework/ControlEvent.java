package it.unitn.disi.test.framework;

import peersim.core.Control;

public class ControlEvent extends BaseEvent {
	
	private final long fPeriod;

	private final FakeEDEngine fParent;
	
	public ControlEvent(FakeEDEngine parent, long time, long period, Object control) {
		super(control, time);
		fParent = parent;
		fPeriod = period;
	}

	@Override
	void dispatch() {
		Control ctrl = (Control) fPayload;
		System.err.println("Run control at time " + time + ".");
		ctrl.execute();
		fParent.schedule(new ControlEvent(fParent, time + fPeriod, fPeriod, fPayload));
	}

}