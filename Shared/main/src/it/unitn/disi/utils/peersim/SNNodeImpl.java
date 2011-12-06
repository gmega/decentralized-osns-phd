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
	 * Social network ID for this node.
	 */
	private int fSNId;

	private long fId;

	/**
	 * Cumulative downtime counter.
	 */
	private long fDownTime;

	/**
	 * Cumulative uptime counter.
	 */
	private long fUptime;

	private int fLogins = 1;

	/**
	 * Timestamp for the last state change.
	 */
	private long fLastChange;

	private final boolean fLogSessions;

	private boolean fActive = false;

	public SNNodeImpl(String prefix) {
		super(prefix);
		fLogSessions = Configuration.contains(prefix + "." + PAR_LOG_SESSIONS);
		fSNId = (int) super.getID();
		fId = super.getID();
		fLastChange = CommonState.getTime();
	}

	public void setFailState(int newState) {
		int oldState = getFailState();
		super.setFailState(newState);
		if (oldState == newState) {
			return;
		}

		// We only get here if:
		// oldState != newState
		// oldState != GeneralNode.DEAD
		switch (newState) {
		case GeneralNode.DEAD:
		case GeneralNode.DOWN:
			// If node was already down, there's
			// nothing to do.
			if (oldState == GeneralNode.DOWN) {
				break;
			}
			// Logs end of uptime.
			logEvent("up");
			fUptime += delta();
			break;

		case GeneralNode.OK:
			// Logs end of downtime.
			logEvent("down");
			fDownTime += delta();
			fLogins++;
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

	public long uptime(boolean includeCurrent) {
		return (includeCurrent && isUp()) ? fUptime + delta() : fUptime;
	}

	public long downtime(boolean includeCurrent) {
		return (includeCurrent && !isUp()) ? fDownTime + delta() : fDownTime;
	}

	public long currentJump() {
		return delta();
	}

	public long uptimeN(boolean includeCurrent) {
		return (!includeCurrent && isUp()) ? fLogins - 1 : fLogins;
	}

	public long downtimeN(boolean includeCurrent) {
		return (isUp() || !includeCurrent) ? fLogins - 1 : fLogins; 
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

	public void setID(long id) {
		fId = id;
	}

	@Override
	public long getID() {
		return fId;
	}

	@Override
	public void setSNId(int id) {
		fSNId = id;
	}

	@Override
	public int getSNId() {
		return fSNId;
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

	private long originalID() {
		return super.getID();
	}

	@Override
	public Object clone() {
		SNNodeImpl cloned = (SNNodeImpl) super.clone();
		cloned.fId = cloned.originalID();
		cloned.fLastChange = CommonState.getTime();
		return cloned;
	}

	private static final INodeStateListener NULL_LISTENER = new INodeStateListener() {
		@Override
		public void stateChanged(int oldState, int newState, SNNode node) {
		}
	};
}
