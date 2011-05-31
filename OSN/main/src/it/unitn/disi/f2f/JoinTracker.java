package it.unitn.disi.f2f;

import peersim.core.Node;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.utils.IReference;

/**
 * {@link JoinTracker} keeps track of the join process by
 * 
 * @author giuliano
 */
public class JoinTracker {

	private final DiscoveryProtocol fParent;

	private int fCopies;
	
	private int fDrops;

	public JoinTracker(DiscoveryProtocol parent) {
		fParent = parent;
	}

	public void copied() {
		fCopies++;
		System.out.println(fCopies);
	}

	public void dropped(IGossipMessage copy, Node location) {
		notifyDrop(copy);
		fDrops++;
		if (fCopies == fDrops) {
			notifyJoinDone(copy);
		}
	}

	private void notifyDrop(IGossipMessage copy) {
		fParent.dropped(copy);
	}

	private void notifyJoinDone(IGossipMessage copy) {
		fParent.joinDone(copy, this);
	}
	
	public int copies() {
		return fCopies;
	}
}
