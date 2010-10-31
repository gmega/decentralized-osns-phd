package it.unitn.disi.utils.peersim;

import peersim.core.CommonState;
import peersim.core.GeneralNode;

public class SNNodeImpl extends GeneralNode implements SNNode {
	
	private long fDownTime;
	
	private long fUptime;
	
	private long fLastChange;

	public SNNodeImpl(String prefix) {
		super(prefix);
	}
	
	public void setFailState(int failState) {
		super.setFailState(failState);
		switch(failState) {
		case GeneralNode.DEAD:
		case GeneralNode.DOWN:
			fUptime += delta();
			break;
		case GeneralNode.OK:
			fDownTime += delta();
			break;
		}
		
		fLastChange = CommonState.getTime();
	}

	private long delta() {
		return CommonState.getTime() - fLastChange;
	}
	
	public void clearUptime() {
		fUptime = 0;
		fLastChange = CommonState.getTime();
	}
	
	public void clearDowntime() {
		fDownTime = 0;
		fLastChange = CommonState.getTime();
	}
	
	public long uptime() {
		return isUp() ? fUptime + delta() : fUptime;
	}
	
	public long downtime() {
		return !isUp() ? fDownTime + delta() : fDownTime;
	}
}
