package it.unitn.disi.churn;

import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.measure.IValueObserver;

/**
 * Helper class for estimating stationary probabilities in a Markov chain.
 * 
 * @author giuliano
 */
public class StationaryEstimator extends TransitionEstimator {

	public StationaryEstimator(final INetworkState state,
			IValueObserver observer) {
		super(state, new INetworkState() {
			@Override
			public boolean holds(INetwork network) {
				return !state.holds(network);
			}
		}, observer);
	}

}
