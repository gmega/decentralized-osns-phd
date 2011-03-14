package it.unitn.disi.utils.peersim;

import peersim.core.CommonState;
import peersim.core.GeneralNode;

public class SNNodeImpl extends GeneralNode implements SNNode {
	
	private INodeStateListener fListener = NULL_LISTENER;
	
	private long fDownTime;
	
	private long fUptime;
	
	private long fLastChange;
	
	public SNNodeImpl(String prefix) {
		super(prefix);
	}
	
	public void setFailState(int newState) {
		int oldState = getFailState();
		super.setFailState(newState);
		if (oldState == newState) {
			return;
		}
		
		switch(newState) {
		case GeneralNode.DEAD:
		case GeneralNode.DOWN:
			fUptime += delta();
			break;
		case GeneralNode.OK:
			fDownTime += delta();
			break;
		}
		
		fLastChange = CommonState.getTime();
		fListener.stateChanged(oldState, newState, this);
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

	@Override
	public void setStateListener(INodeStateListener listener) {
		fListener = listener;
	}

	@Override
	public void clearStateListener() {
		fListener = NULL_LISTENER;
	}
	
	private static final INodeStateListener NULL_LISTENER = new INodeStateListener() {
		@Override
		public void stateChanged(int oldState, int newState, SNNode node) { }
	};
}
