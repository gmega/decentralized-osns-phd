package it.unitn.disi.utils.peersim;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.GeneralNode;

public class SNNodeImpl extends GeneralNode implements SNNode {

	private static final String PRINT_PREFIX = SNNodeImpl.class.getSimpleName();

	private static final String PAR_LOG_SESSIONS = "logsessions";

	// Awful. I desperately need a tabular log writing infrastructure.
	private static boolean fPrintedHeader = false;

	private INodeStateListener fListener = NULL_LISTENER;

	/**
	 * Cumulative downtime counter.
	 */
	private long fDownTime;

	/**
	 * Cumulative uptime counter.
	 */
	private long fUptime;

	/**
	 * Session counter.
	 */
	private int fSessionCount;

	/**
	 * Timestamp for the last state change.
	 */
	private long fLastChange;

	private final boolean fLogSessions;
	
	private boolean fActive = false;

	public SNNodeImpl(String prefix) {
		super(prefix);
		fLogSessions = Configuration.contains(prefix + "." + PAR_LOG_SESSIONS);
	}

	public void setFailState(int newState) {
		int oldState = getFailState();
		super.setFailState(newState);
		if (oldState == newState) {
			return;
		}

		switch (newState) {
		case GeneralNode.DEAD:
		case GeneralNode.DOWN:
			// Logs end of uptime.
			logEvent("up");
			fUptime += delta();
			break;

		case GeneralNode.OK:
			// Logs end of downtime.
			logEvent("down");
			fDownTime += delta();
			fSessionCount++;
			break;
		}

		fLastChange = CommonState.getTime();
		fListener.stateChanged(oldState, newState, this);
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

	@Override
	public long lastStateChange() {
		return fLastChange;
	}
	
	@Override
	public boolean isActive() {
		return fActive;
	}
	
	public void active(boolean stats) {
		fActive = stats;
	}

	private void logEvent(String type) {
		if (!fLogSessions) {
			return;
		}
		header();
		StringBuffer sb = new StringBuffer(PRINT_PREFIX);
		sb.append(":");
		sb.append(getID());
		sb.append(" ");
		sb.append(type);
		sb.append(" ");
		sb.append(CommonState.getTime() - fLastChange);
		sb.append(" ");
		sb.append(CommonState.getTime());
		System.out.println(sb.toString());
	}

	private void header() {
		if (!fLogSessions || fPrintedHeader) {
			return;
		}
		fPrintedHeader = true;
		StringBuffer sb = new StringBuffer(PRINT_PREFIX);
		sb.append(":");
		sb.append("id");
		sb.append(" ");
		sb.append("type");
		sb.append(" ");
		sb.append("duration");
		sb.append(" ");
		sb.append("time");
		System.out.println(sb.toString());
	}

	private long delta() {
		return CommonState.getTime() - fLastChange;
	}

	private static final INodeStateListener NULL_LISTENER = new INodeStateListener() {
		@Override
		public void stateChanged(int oldState, int newState, SNNode node) {
		}
	};
}
