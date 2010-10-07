package it.unitn.disi.sps.cyclon;

import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.collections.IExchanger;
import it.unitn.disi.utils.peersim.PeersimUtils;

import java.util.ArrayList;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.extras.am.epidemic.EpidemicProtocol;
import peersim.extras.am.epidemic.Message;

@AutoConfig
public class CyclonSN implements Linkable, EpidemicProtocol, IExchanger {

	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------

	static class Parameters {
		public int oneHopPid;

		public int twoHopPid;

		public int ownPid;

		public int viewSize;
	}

	static Parameters fPars;

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	private ArrayList<NodeDescriptor> fView;

	private CyclonMessage fRequestMsg;

	private CyclonMessage fReplyMsg;

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
		}

		fView = new ArrayList<NodeDescriptor>(viewSize);
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
		if (fView.size() == 0) {
			return null;
		}
		// Returns the node with the highest age.
		NodeDescriptor max = fView.get(0);
		for (int i = 1; i < fView.size(); i++) {
			NodeDescriptor candidate = fView.get(i);
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
			if (fView.size() < fPars.viewSize) {
				fView.add(candidate);
			}

			// ... if not enough are available, goes for the node descriptors
			// that we sent with the pair message.
			else {
				// We don't have any more available free slots. Stop.
				if (index >= pair.size()) {
					break;
				}
				
				// Replacement might not succeed if the view has been
				// changed in the meantime between the a prepareRequest
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
		OrderingUtils.permute(0, fView.size(), this, CommonState.r);

		// 3. Tries to pick l - 1 nodes from our view that are direct
		// friends with the receiver.
		Linkable oneHop = (Linkable) receiver.getProtocol(fPars.oneHopPid);
		fillPayload(message, oneHop);

		// 4. If those were not enough, tries to fill in the remaining gaps with
		// friends-of-friends that we share with the receiver.
		Linkable twoHop = (Linkable) receiver.getProtocol(fPars.twoHopPid);
		fillPayload(message, twoHop);

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
	 */

	private void fillPayload(CyclonMessage message, Linkable filter) {
		for (int i = 0; i < fView.size() && !message.isFull(); i++) {
			NodeDescriptor candidate = fView.get(i);
			if (filter.contains(candidate.node())) {
				message.append(NodeDescriptor.cloneFrom(candidate));
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
			return;
		}

		NodeDescriptor replacement = fView.remove(fView.size() - 1);
		// If we removed the element we had to remove anyways, we're done.
		if (index != fView.size()) {
			// Otherwise put a replacement where the previous element
			// was.
			fView.set(index, replacement);
		}
	}

	// ----------------------------------------------------------------------

	private int descriptorIndexOf(Node node) {
		for (int i = 0; i < fView.size(); i++) {
			NodeDescriptor descriptor = fView.get(i);
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
		fView.set(idx, neuw);
		return true;
	}

	// ----------------------------------------------------------------------

	private void increaseAge() {
		for (NodeDescriptor descriptor : fView) {
			descriptor.increaseAge();
		}
	}

	// ----------------------------------------------------------------------
	// Linkable interface.
	// ----------------------------------------------------------------------

	@Override
	public boolean addNeighbor(Node neighbour) {
		if (fView.size() < fPars.viewSize) {
			fView.add(new NodeDescriptor(neighbour, 0));
			return true;
		}

		return false;
	}

	// ----------------------------------------------------------------------

	@Override
	public boolean contains(Node neighbor) {
		for (int i = 0; i < fView.size(); i++) {
			if (neighbor.equals(fView.get(i).node())) {
				return true;
			}
		}

		return false;
	}

	// ----------------------------------------------------------------------

	@Override
	public int degree() {
		return fView.size();
	}

	// ----------------------------------------------------------------------

	@Override
	public Node getNeighbor(int i) {
		return fView.get(i).node();
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

	@Override
	public void exchange(int i, int j) {
		NodeDescriptor tmp = fView.get(i);
		fView.set(i, fView.get(j));
		fView.set(j, tmp);
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------
	public Object clone() {
		try {
			CyclonSN clone = (CyclonSN) super.clone();
			clone.fView = new ArrayList<NodeDescriptor>(fPars.viewSize);
			for (int i = 0; i < fView.size(); i++) {
				clone.fView.set(i, NodeDescriptor.cloneFrom(fView.get(i)));
			}
			clone.fRequestMsg = CyclonMessage.cloneFrom(fRequestMsg);
			clone.fRequestMsg = CyclonMessage.cloneFrom(fReplyMsg);
			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
