package it.unitn.disi.f2f;

import it.unitn.disi.network.SizeConstants;
import it.unitn.disi.utils.peersim.BitSetNeighborhood;
import peersim.core.CommonState;
import peersim.core.Node;

class AggregationTreeState {
	
	private final AdvertisementMessage fParentMsg;

	private Node fParent;

	private final BitSetNeighborhood fPending;

	private final BitSetNeighborhood fPushed;

	private int fRefreshTime;

	private boolean fActive;
	
	private boolean fCollected;

	private boolean fTryParent;

	public AggregationTreeState(Node parent, BitSetNeighborhood filter,
			BitSetNeighborhood push, AdvertisementMessage parentMsg) {
		fPending = push;
		fPushed = filter;
		fParent = parent;
		fParentMsg = parentMsg;
		fTryParent = (parent != null);
		fRefreshTime = CommonState.getIntTime();
	}

	public int merge(BitSetNeighborhood neighborhood) {
		return neighborhood.addAll(this.fPending);
	}

	public void done() {
		this.allPushed();
	}

	public void mergeAndForward(AggregationTreeState other) {
		fRefreshTime = CommonState.getIntTime();

		// First transfers from parent to us.
		other.merge(fPending);

		// Now filters what we have to push down.
		fPending.setDifference(fPushed);

		// Updates the parent on what's been pushed.
		other.allPushed();

		// Updates the parent message, if any.
		if (fParentMsg != null) {
			fParentMsg.neighborhood().addAll(fPending);
		}
	}
	
	public AdvertisementMessage propagationMessage() {
		return fParentMsg;
	}
	
	private void allPushed() {
		fPushed.addAll(fPending);
		fPending.removeAll();
	}

	public void clearPending() {
		fPending.removeAll();
	}

	public int pendingDescriptors() {
		return fPending.degree();
	}
	
	public int sizeOf() {
		return pendingDescriptors() * SizeConstants.IPV4_ADDRESS_SIZE;
	}

	public int age() {
		return CommonState.getIntTime() - fRefreshTime;
	}

	public BitSetNeighborhood toPush() {
		return fPending;
	}

	public int sequenceNumber() {
		return fParentMsg.sequenceNumber();
	}

	public Node parent() {
		return fParent;
	}

	public Node originator() {
		return fParentMsg.originator();
	}

	public boolean shouldTryParent() {
		return fTryParent;
	}

	public void parentFailed() {
		fTryParent = false;
	}

	public boolean isTree(Node originator, int sequence) {
		return sequence == sequenceNumber() && originator == originator();
	}
	
	public boolean isDirty() {
		return fPending.degree() != 0;
	}
	
	public void collected() {
		fCollected = true;
		aggregationDone();
	}
	
	public boolean isCollected() {
		return fCollected;
	}
	
	/**
	 * Allows the parent protocol to flag this {@link TreeState} as scheduled.
	 * 
	 * @param state
	 */
	public void setQueued(boolean state) {
		fActive = state;
		aggregationDone();
	}

	/**
	 * @return whether this tree is scheduled for transmission or not.
	 */
	public boolean isQueued() {
		return fActive;
	}
	
	private void aggregationDone() {
		if (fCollected && !fActive) {
			JoinTracker tracker = fParentMsg.tracker();
			tracker.aggregationStop(this);
		}
	}
}
