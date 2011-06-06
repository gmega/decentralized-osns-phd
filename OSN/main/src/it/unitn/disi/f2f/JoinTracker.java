package it.unitn.disi.f2f;

import peersim.core.CommonState;
import peersim.core.Node;
import peersim.util.IncrementalStats;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.utils.logging.StructuredLog;

/**
 * {@link JoinTracker} keeps track of individual join processes. The object
 * should be attached to all {@link AdvertisementMessage} instances created for
 * this join process.
 * 
 * @author giuliano
 */
@StructuredLog(key = "JOIN", fields = { "id", "degree", "join", "seen",
		"unseen", "stale" })
public class JoinTracker {

	private final DiscoveryProtocol fParent;

	private int fStartTime;

	private IncrementalStats fStats;

	private int fCopies;

	private int fDrops;

	public JoinTracker(DiscoveryProtocol parent) {
		fParent = parent;
		fStartTime = CommonState.getIntTime();
	}

	public void copied() {
		fCopies++;
	}

	public void dropped(IGossipMessage copy, Node location) {
		fDrops++;
		if (fCopies == fDrops) {
			notifyJoinDone(copy);
		}

		// Prints the statistics.
	}

	private void notifyJoinDone(IGossipMessage copy) {
		fParent.joinDone(copy, this);
	}

	public void descriptorsHomed(int howmany) {
		fStats.add(fStartTime - CommonState.getIntTime(), howmany);
	}

	public int copies() {
		return fCopies;
	}
}
