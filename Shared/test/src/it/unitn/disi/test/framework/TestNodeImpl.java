package it.unitn.disi.test.framework;

import it.unitn.disi.utils.peersim.INodeStateListener;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.ArrayList;

import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.GeneralNode;
import peersim.core.Protocol;

public class TestNodeImpl implements SNNode {

	private static int fIds;

	public static void resetIDCounter() {
		fIds = 0;
	}

	private ArrayList<Protocol> fProtocols = new ArrayList<Protocol>();

	private int fFailState;

	private int fIndex;

	private long fId;

	public TestNodeImpl() {
		fIndex = -1;
		fId = fIds++;
		fFailState = Fallible.OK;
	}

	public int addProtocol(Protocol p) {
		int pid = fProtocols.size();
		fProtocols.add(p);
		return pid;
	}

	@Override
	public int getFailState() {
		return fFailState;
	}

	@Override
	public boolean isUp() {
		return fFailState == Fallible.OK;
	}

	@Override
	public Protocol getProtocol(int i) {
		return fProtocols.get(i);
	}

	@Override
	public int protocolSize() {
		return fProtocols.size();
	}

	@Override
	public void setIndex(int index) {
		fIndex = index;
	}

	@Override
	public int getIndex() {
		return fIndex;
	}

	@Override
	public long getID() {
		return fId;
	}

	public Object clone() {
		TestNodeImpl result = null;
		try {
			result = (TestNodeImpl) super.clone();
		} catch (CloneNotSupportedException e) {
		}

		result.fProtocols = new ArrayList<Protocol>();
		result.fId = fIds++;
		CommonState.setNode(result);
		for (int i = 0; i < fProtocols.size(); ++i) {
			CommonState.setPid(i);
			result.fProtocols.add((Protocol) fProtocols.get(i).clone());
		}
		return result;
	}

	public String toString() {
		return "ID: " + fId + ", " + (isUp() ? "UP" : "DOWN");
	}

	// ------------------------------------------------------------------------
	// UGLY: This is copy-and-pasted code from SNNodeImpl. Ideally we should be
	// able to use SNNodeImpl, but it subclasses GeneralNode, and is thus
	// coupled to the PeerSim singleton tangle.
	// ------------------------------------------------------------------------
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

	public void setFailState(int newState) {
		int oldState = getFailState();
		fFailState = newState;
		if (oldState == newState) {
			return;
		}

		switch (newState) {
		case GeneralNode.DEAD:
		case GeneralNode.DOWN:
//			System.out.println("Node " + getID() + " DOWN @"
//					+ CommonState.getTime());
			fUptime += delta();
			break;

		case GeneralNode.OK:
//			System.out.println("Node " + getID() + " OK @"
//					+ CommonState.getTime());
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

	private long delta() {
		return CommonState.getTime() - fLastChange;
	}

	private static final INodeStateListener NULL_LISTENER = new INodeStateListener() {
		@Override
		public void stateChanged(int oldState, int newState, SNNode node) {
		}
	};

	@Override
	public boolean isActive() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void active(boolean active) {
		// TODO Auto-generated method stub
		
	}
}
