package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IGossipMessage;
import peersim.core.CommonState;
import peersim.core.Node;

/**
 * {@link JoinTracker} keeps track of individual join processes. The object
 * should be attached to all {@link AdvertisementMessage} instances created for
 * this join process.
 * 
 * @author giuliano
 */
public class JoinTracker {

	private final DiscoveryProtocol fParent;

	private int fStartTime;
	
	private int fPropagators;
	
	private int fAggregators;

	private int fPropagationActive;

	private int fAggregationActive;

	public JoinTracker(DiscoveryProtocol parent) {
		fParent = parent;
		fStartTime = CommonState.getIntTime();
	}

	void joinPropagation() {
		fPropagators++;
		fPropagationActive++;
	}

	void joinAggregation() {
		fAggregators++;
		fAggregationActive++;
	}

	void propagationStop(IGossipMessage copy, Node location) {
		fPropagationActive--;
		// Notify the proper protocol.
		DiscoveryProtocol protocol = (DiscoveryProtocol) location
				.getProtocol(fParent.pid());
		protocol.dropped(copy);
		if (done()) {
			joinDone(copy);
		}
	}

	void aggregationStop(AggregationTreeState state) {
		fAggregationActive--;
		if (done()) {
			joinDone(state.propagationMessage());
		}
	}

	private boolean done() {
		return fAggregationActive == 0 && fPropagationActive == 0;
	}

	private void joinDone(IGossipMessage copy) {
		fParent.joinDone(copy, this);
	}
	
	public int propagators() {
		return fPropagators;
	}
	
	public int aggregators() {
		return fAggregators;
	}

	public DiscoveryProtocol parent() {
		return fParent;
	}
	
	public int totalTime() {
		return CommonState.getIntTime() - fStartTime;
	}
}
