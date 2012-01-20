package it.unitn.disi.unitsim.experiments;

import it.unitn.disi.utils.peersim.PeersimUtils;
import peersim.core.CommonState;

/**
 * Simple utility class for keeping track of burn-in time for the experiments
 * that require it.
 * 
 * @author giuliano
 * 
 */
class BurnInSupport {

	final private int fId;

	final private long fBurninTime;

	final private long fStartTime;

	private boolean fIsBurningIn;

	private boolean fWasBurningIn;

	BurnInSupport(long burnin, int id) {
		fStartTime = CommonState.getTime();
		fBurninTime = burnin;
		fIsBurningIn = true;
		fWasBurningIn = true;
		fId = id;

		System.err
				.println("-- Start burn-in period for experiment " + id + ".");
	}

	public boolean isBurningIn() {
		if (!fIsBurningIn) {
			return false;
		}

		if (ellapsedTime() < fBurninTime) {
			return true;
		}

		fIsBurningIn = false;

		System.err.println("-- Burn-in period for experiment " + fId
				+ " over (total: " + ellapsedTime() + ", excess: "
				+ (ellapsedTime() - fBurninTime) + ").");
		System.err.println("-- Active nodes: " + PeersimUtils.countActives()
				+ ".");

		return false;
	}

	private long ellapsedTime() {
		return CommonState.getTime() - fStartTime;
	}

	public boolean wasBurningIn() {
		if (!fWasBurningIn) {
			return false;
		}
		fWasBurningIn = false;
		return true;
	}

}
