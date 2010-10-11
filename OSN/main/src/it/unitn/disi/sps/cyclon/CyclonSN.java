package it.unitn.disi.sps.cyclon;

import java.util.NoSuchElementException;

import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.peersim.PeersimUtils;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.extras.am.epidemic.EpidemicProtocol;
import peersim.extras.am.epidemic.Message;

@AutoConfig
public class CyclonSN implements Linkable, EpidemicProtocol {

	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------

	static class Parameters {
		public int oneHopPid;

		public int twoHopPid;

		public int ownPid;

		public int viewSize;
	}

	private static Parameters fPars;

	// ----------------------------------------------------------------------
	// Shared data structures.
	// ----------------------------------------------------------------------

	private static NodeDescriptor[] fStorage;

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	private NodeDescriptor[] fView;

	private int fViewSize;

	private CyclonMessage fRequestMsg;

	private CyclonMessage fReplyMsg;
	
	private ContactTracker fContactTracker;

	// ----------------------------------------------------------------------

	public CyclonSN(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("one_hop") int oneHopPid,
			@Attribute("two_hop") int twoHopPid,
			@Attribute("view_size") int viewSize, @Attribute("l") int l) {

		// Shared protocol parameters.
		if (fPars == null) {
			fPars = new Parameters();
			fPars.oneHopPid = oneHopPid;
			fPars.twoHopPid = twoHopPid;
			fPars.ownPid = PeersimUtils.selfPid(prefix);
			fPars.viewSize = viewSize;
			fStorage = new NodeDescriptor[viewSize];
		}

		fView = new NodeDescriptor[viewSize];
		fRequestMsg = new CyclonMessage(l);
		fReplyMsg = new CyclonMessage(l);
		fRequestMsg.setRequest(true);
	}

	// ----------------------------------------------------------------------
	// EpidemicProtocol interface.
	// ----------------------------------------------------------------------

	@Override
	public Node selectPeer(Node lnode) {
		// XXX Note that this implementation is NOT prepared to handle
		// more than one selectPeer call per cycle.
		if (fViewSize == 0) {
			return null;
		}
		// Returns the node with the highest age.
		NodeDescriptor max = fView[0];
		for (int i = 1; i < fViewSize; i++) {
			NodeDescriptor candidate = fView[i];
			if (candidate.age() > max.age()) {
				max = candidate;
			}
		}

		return max.node();
	}

	// ----------------------------------------------------------------------

	@Override
	public Message prepareRequest(Node sender, Node receiver) {
		increaseAge();
		removeDescriptor(receiver);
		return populateMessage(fRequestMsg, sender, receiver);
	}

	// ----------------------------------------------------------------------

	@Override
	public Message prepareResponse(Node responder, Node requester,
			Message request) {
		// Debugging.
		registerContact(responder, requester);
		return populateMessage(fReplyMsg, responder, requester);
	}

	// ----------------------------------------------------------------------

	@Override
	public void merge(Node receiver, Node sender, Message msg) {

		CyclonMessage received = (CyclonMessage) msg;
		// Gets the message that "pairs" with this one.
		CyclonMessage pair = msg.isRequest() ? fReplyMsg : fRequestMsg;

		int index = 1;
		for (int i = 0; i < received.size(); i++) {
			NodeDescriptor candidate = received.getDescriptor(i);
			Node candidateNode = candidate.node();

			// If entry points to our own node, or if there's an entry with that
			// node already in our view, ignores it.
			if (candidateNode.equals(receiver) || viewContains(candidateNode)) {
				continue;
			}

			// Otherwise, first tries to use an empty cache slot, and...
			if (fViewSize < fPars.viewSize) {
				fView[fViewSize++] = candidate;
			}

			// ... if not enough are available, goes for the node descriptors
			// that we sent with the pair message.
			else {
				// We don't have any more available free slots. Stop.
				if (index >= pair.size()) {
					break;
				}

				// Replacement might not succeed if the view has been
				// changed in the window between a prepareRequest
				// and a merge.
				if (!replace(pair.getDescriptor(index++), candidate)) {
					i--;
				}
			}
		}

		// Releases the pair message.
		pair.release();
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private CyclonMessage populateMessage(CyclonMessage message, Node sender,
			Node receiver) {

		// Acquires the message.
		message.acquire();

		// 1. Adds a fresh entry pointing to ourselves.
		message.append(new NodeDescriptor(sender));

		// 2. Shuffles our view.
		OrderingUtils.permute(0, fViewSize, fView, CommonState.r);

		// 3. Tries to pick l - 1 nodes from our view that are direct
		// friends with the receiver.
		int removed = fillPayload(message, onehop(receiver), true);

		// 4. If those were not enough, tries to fill in the remaining gaps with
		// friends-of-friends that we share with the receiver.
		Linkable twoHop = (Linkable) receiver.getProtocol(fPars.twoHopPid);
		fillPayload(message, twoHop, false);

		// Puts the descriptors removed in step (3) back into the view.
		restoreView(removed);

		return message;
	}

	// ----------------------------------------------------------------------

	/**
	 * Fills in a message payload with descriptors that are:
	 * <ol>
	 * <li>contained in the view;</li>
	 * <li>whose {@link Node} is a part of another {@link Linkable}</li>
	 * </ol>
	 * 
	 * Additionally, stores the view indices of the selected elements.
	 * 
	 * @param target
	 *            the {@link CyclonMessage} into which store the data.
	 * @param filter
	 *            the filtering {@link Linkable}. Only node descriptors
	 *            returning nodes for which {@link Linkable#contains(Node)}
	 *            returns <code>true</code> will be considered.
	 * @param removeSelected
	 *            if set to <code>true</code>, causes the selected descriptors
	 *            to be set to <code>null</code> in the view, but without
	 *            changing the view size. Removed elements will be transferred
	 *            to a storage array ({@link #fStorage}).
	 * 
	 * @return the number of elements added to the message (and removed from the
	 *         view, if removeSelected was set to <code>true</code>).
	 */

	private int fillPayload(CyclonMessage message, Linkable filter,
			boolean removeSelected) {
		int added = 0;
		for (int i = 0; i < fViewSize && !message.isFull(); i++) {
			NodeDescriptor candidate = fView[i];
			if (candidate != null && filter.contains(candidate.node())) {
				message.append(NodeDescriptor.cloneFrom(candidate));
				if (removeSelected) {
					fStorage[added++] = fView[i];
					fView[i] = null;
				}
			}
		}
		return added;
	}

	// ----------------------------------------------------------------------

	private void restoreView(int removed) {
		int k = 0;
		for (int i = 0; i < fViewSize && k != removed; i++) {
			if (fView[i] == null) {
				fView[i] = fStorage[k++];
			}
		}
	}

	// ----------------------------------------------------------------------

	private boolean viewContains(Node candidateNode) {
		return descriptorIndexOf(candidateNode) != -1;
	}

	// ----------------------------------------------------------------------

	private void removeDescriptor(Node node) {
		int index = descriptorIndexOf(node);
		if (index == -1) {
			throw new NoSuchElementException();
		}

		fView[index] = null;
		fViewSize--;

		if (index != fViewSize) {
			fView[index] = fView[--fViewSize];
		}
	}

	// ----------------------------------------------------------------------

	private int descriptorIndexOf(Node node) {
		for (int i = 0; i < fViewSize; i++) {
			NodeDescriptor descriptor = fView[i];
			if (descriptor.node().equals(node)) {
				return i;
			}
		}
		return -1;
	}

	// ----------------------------------------------------------------------

	private boolean replace(NodeDescriptor old, NodeDescriptor neuw) {
		int idx = descriptorIndexOf(old.node());
		if (idx == -1) {
			return false;
		}
		fView[idx] = neuw;
		return true;
	}

	// ----------------------------------------------------------------------

	private void increaseAge() {
		for (int i = 0; i < fViewSize; i++) {
			fView[i].increaseAge();
		}
	}

	// ----------------------------------------------------------------------
	// Linkable interface.
	// ----------------------------------------------------------------------

	@Override
	public boolean addNeighbor(Node neighbour) {
		if (fViewSize < fPars.viewSize) {
			fView[fViewSize++] = new NodeDescriptor(neighbour, 0);
			return true;
		}

		return false;
	}

	// ----------------------------------------------------------------------

	@Override
	public boolean contains(Node neighbor) {
		for (int i = 0; i < fViewSize; i++) {
			if (neighbor.equals(fView[i].node())) {
				return true;
			}
		}

		return false;
	}

	// ----------------------------------------------------------------------

	@Override
	public int degree() {
		return fViewSize;
	}

	// ----------------------------------------------------------------------

	@Override
	public Node getNeighbor(int i) {
		return fView[i].node();
	}

	// ----------------------------------------------------------------------

	@Override
	public void onKill() {
		fView = null;
	}

	// ----------------------------------------------------------------------

	@Override
	public void pack() {
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------
	public Object clone() {
		try {
			CyclonSN clone = (CyclonSN) super.clone();
			clone.fView = new NodeDescriptor[fPars.viewSize];
			for (int i = 0; i < fViewSize; i++) {
				clone.fView[i] = NodeDescriptor.cloneFrom(fView[i]);
			}
			clone.fRequestMsg = CyclonMessage.cloneFrom(fRequestMsg);
			clone.fRequestMsg = CyclonMessage.cloneFrom(fReplyMsg);
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	// ----------------------------------------------------------------------
	// Convenience accessors.
	// ----------------------------------------------------------------------

	protected Linkable onehop(Node node) {
		return (Linkable) node.getProtocol(fPars.oneHopPid);
	}
	
	protected Linkable twohop(Node node) {
		return (Linkable) node.getProtocol(fPars.twoHopPid);
	}
	
	protected CyclonSN cyclon(Node node) {
		return (CyclonSN) node.getProtocol(fPars.ownPid);
	}

	// ----------------------------------------------------------------------
	// Debugging methods.
	// ----------------------------------------------------------------------
	
	public void enableContactTracking() {
		if (fContactTracker == null) {
			fContactTracker = new ContactTracker(this);
		}
	}
	
	// ----------------------------------------------------------------------
	
	private void registerContact(Node contacted, Node initiator) {
		if (contacted.getID() == 299) {
			enableContactTracking();
		}
		
		if (fContactTracker == null) {
			return;
		}
		
		Linkable onehop = (Linkable) contacted.getProtocol(fPars.oneHopPid);
		Linkable twohop = (Linkable) contacted.getProtocol(fPars.twoHopPid);
		
		if (onehop.contains(initiator)) {
			fContactTracker.traceOnehopContact(contacted, initiator);
		} else if (twohop.contains(initiator)) {
			fContactTracker.traceTwohopContact(contacted, initiator);
		} else {
			throw new IllegalStateException();
		}
	}

}

class ContactTracker {
	
	public final CyclonSN fParent;

	public ContactTracker(CyclonSN parent) {
		fParent = parent;
	}

	public void traceOnehopContact(Node contacted, Node initiator) {
		System.out.println("F1 " + contacted.getID() + " " + initiator.getID()
				+ " " + countIndegree(contacted, false)
				+ " " + CommonState.getTime());
	}

	public void traceTwohopContact(Node contacted, Node initiator) {
		System.out.println("F2 " + contacted.getID() + " " + initiator.getID()
				+ " " + countIndegree(contacted, true)
				+ " " + CommonState.getTime());
	}

	private int countIndegree(Node contacted, boolean twohopIndegree) {
		int indegree = 0;
		Linkable onehop = fParent.onehop(contacted);
		Linkable twohop = fParent.twohop(contacted);
		
		Linkable filter = twohopIndegree ? twohop : onehop;
		
		for (int i = 0; i < filter.degree(); i++) {
			Node candidate = filter.getNeighbor(i);
			if (twohopIndegree && onehop.contains(candidate)) {
				continue;
			}
			
			CyclonSN neighbor = fParent.cyclon(candidate);
			if (neighbor.contains(contacted)) {
				indegree++;
			}
		}
		return indegree;
	}
}