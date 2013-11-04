package it.unitn.disi.simulator.core;

import java.util.ArrayList;

public abstract class IProcess extends Schedulable {

	private static final long serialVersionUID = -6326282369880873309L;

	public static int PROCESS_SCHEDULABLE_TYPE = 0;

	public static enum State {
		up, down;
	}

	private final ArrayList<Object> fProtocols;

	public IProcess() {
		this(null);
	}

	public IProcess(Object[] protocols) {
		fProtocols = new ArrayList<Object>();
		if (protocols != null) {
			for (Object protocol : protocols) {
				fProtocols.add(protocol);
			}
		}
	}

	/**
	 * @param clock
	 *            the simulation {@link IClockData}.
	 * 
	 * @return the empirical (simulated) availability for this node.
	 */
	public double empiricalAvailability(IClockData clock) {
		return uptime(clock) / clock.rawTime();
	}

	/**
	 * Returns the asymptotic availability of this {@link IProcess}, or
	 * {@link Double#NaN} if it not known or undefined.
	 */
	public abstract double asymptoticAvailability();

	/**
	 * @return returns the actual uptime of the current node.
	 */
	public abstract double uptime(IClockData clock);

	/**
	 * @return whether or not the node is currently up.
	 */
	public abstract boolean isUp();

	/**
	 * @return one of {@link State#up} or {@link State#down}.
	 */
	public abstract State state();

	/**
	 * @return the numeric id of this node.
	 */
	public abstract int id();

	/**
	 * @param id
	 *            the numeric id of a protocol.
	 * @return a protocol object associated to this process.
	 */
	public Object getProtocol(int id) {
		return fProtocols.get(id);
	}

	/**
	 * Adds a protocol to this {@link IProcess} and returns its pid.
	 */
	public int addProtocol(Object protocol) {
		fProtocols.add(protocol);
		return fProtocols.size() - 1;
	}

	@Override
	public int type() {
		return PROCESS_SCHEDULABLE_TYPE;
	}
}