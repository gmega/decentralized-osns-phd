package it.unitn.disi.churn.simulator;

public abstract class IProcess extends Schedulable {
	
	public static int PROCESS_SCHEDULABLE_TYPE = 0;
	
	public static enum State {
		up, down;
	}

	/**
	 * @return returns the actual uptime of the current node.
	 */
	public abstract double uptime(SimpleEDSim parent);
	
	/**
	 * @return whether or not the noode is currently up.
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

	@Override
	public int type() {
		return PROCESS_SCHEDULABLE_TYPE;
	}
}