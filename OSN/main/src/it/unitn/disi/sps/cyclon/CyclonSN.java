package it.unitn.disi.sps.cyclon;

import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.SimpleFSM;
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
	// Simple FSM for debugging protocol state.
	// ----------------------------------------------------------------------

	private static enum ProtocolState {
		IDLE, REQUEST_SENT, REPLY_SENT;

		public ProtocolState transition(ProtocolState next) {
			return fStateMachine.checkedTransition(this, next);
		}
	}
	
	private static final SimpleFSM<ProtocolState> fStateMachine = new SimpleFSM<ProtocolState>(
			ProtocolState.class);

	static {
		fStateMachine.allowTransitionFrom(ProtocolState.IDLE).to(
				ProtocolState.REQUEST_SENT, ProtocolState.REPLY_SENT);
		fStateMachine.allowTransitionFrom(ProtocolState.REQUEST_SENT).to(
				ProtocolState.IDLE);
		fStateMachine.allowTransitionFrom(ProtocolState.REPLY_SENT).to(
				ProtocolState.IDLE);
	}

	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------

	static class Parameters {
		public int oneHopPid;

		public int twoHopPid;

		public int ownPid;

		public int viewSize;

		public int l;
	}

	static Parameters fPars;

	// ----------------------------------------------------------------------
	// State.
	// ----------------------------------------------------------------------

	private ArrayList<NodeDescriptor> fView;
	
	private CyclonMessage fRequestMsg;

	private CyclonMessage fReplyMsg;
	
	private ProtocolState fState;

	// ----------------------------------------------------------------------

	public CyclonSN(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("one_hop") int oneHopPid,
			@Attribute("two_hop") int twoHopPid,
			@Attribute("view_size") int viewSize, 
			@Attribute("l") int l) {

		// Shared protocol parameters.
		if (fPars == null) {
			fPars = new Parameters();
			fPars.oneHopPid = oneHopPid;
			fPars.twoHopPid = twoHopPid;
			fPars.ownPid = PeersimUtils.selfPid(prefix);
			fPars.viewSize = viewSize;
			fPars.l = l;
		}

		fView = new ArrayList<NodeDescriptor>(viewSize);
		fRequestMsg = new CyclonMessage(l);
		fReplyMsg = new CyclonMessage(l);
		fState = ProtocolState.IDLE;
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
		changeState(ProtocolState.REQUEST_SENT);
		increaseAge();
		removeDescriptor(receiver);
		return populateMessage(fRequestMsg, sender, receiver);
	}

	// ----------------------------------------------------------------------
	
	@Override
	public Message prepareResponse(Node responder, Node requester,
			Message request) {
		changeState(ProtocolState.REPLY_SENT);
		return populateMessage(fReplyMsg, responder, requester);
	}
	
	// ----------------------------------------------------------------------

	@Override
	public void merge(Node receiver, Node sender, Message msg) {
		changeState(ProtocolState.IDLE);
		
		CyclonMessage received = (CyclonMessage) msg;
		CyclonMessage lastSent = pendingMessage();

		int index = 0;

		for (int i = 0; i < received.size(); i++) {
			NodeDescriptor candidate = received.getDescriptor(i);
			Node candidateNode = candidate.node();

			// If entry points to our own node, or if there's an entry with that
			// node already in our view, ignores it.
			if (candidateNode.equals(receiver) || viewContains(candidateNode)) {
				continue;
			}

			// Otherwise, first tries to use an empty cache slot.
			if (fView.size() < fPars.viewSize) {
				fView.add(candidate);
			}
			// If unavailable, goes for the node descriptors that we sent with
			// the last message.
			else {
				// We don't have any more available free slots. Stop.
				if (index > lastSent.size()) {
					break;
				}
				checkedSet(lastSent, index++, candidate);
			}
		}
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private CyclonMessage populateMessage(CyclonMessage message, Node sender,
			Node receiver) {
		
		// 1. Adds a fresh entry pointing to ourselves.
		message.setDescriptor(0, new NodeDescriptor(sender));

		// 2. Shuffles our view.
		OrderingUtils.permute(0, fView.size(), this, CommonState.r);

		// 3. Tries to pick l - 1 nodes from our view that are direct
		// friends with the receiver.
		Linkable oneHop = (Linkable) receiver.getProtocol(fPars.oneHopPid);
		int offset = fillPayload(message, 1, fPars.l - 1, oneHop);

		// 4. If those were not enough, tries to fill in the remaining gaps with
		// friends-of-friends that we share with the receiver.
		Linkable twoHop = (Linkable) receiver.getProtocol(fPars.twoHopPid);
		fillPayload(message, offset, fPars.l - 1 - offset, twoHop);
		
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
	 * @param offset
	 *            the offset of where to start filling the payload vector.
	 * @param length
	 *            the number of elements to be inserted.
	 * @param filter
	 *            the filtering {@link Linkable}. Only node descriptors
	 *            returning nodes for which {@link Linkable#contains(Node)}
	 *            returns <code>true</code> will be considered.
	 * @return the number of elements actually inserted. Might be smaller than
	 *         lenght.
	 */
	private int fillPayload(CyclonMessage target, int offset, int length,
			Linkable filter) {
		int end = offset + length;
		int added = 0;
		for (int i = offset; i < end; i++) {
			int viewIndex = i - offset;
			NodeDescriptor candidate = fView.get(viewIndex);
			if (filter.contains(candidate.node())) {
				target.setDescriptor(i, NodeDescriptor.cloneFrom(candidate));
				target.setIndex(i, viewIndex);
				added++;
			}
		}
		return added;
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
		
		fView.set(index, null);
		NodeDescriptor replacement = fView.remove(fView.size() - 1);
		if (fView.size() > 0) {
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
	
	private void checkedSet(CyclonMessage source, int index, NodeDescriptor value) {
		int viewIndex = source.getIndex(index);
		NodeDescriptor oldDescriptor = fView.get(viewIndex);
		if (!oldDescriptor.node().equals(value.node())) {
			throw new IllegalStateException("Internal error.");
		}

		fView.set(viewIndex, value);
	}
	
	// ----------------------------------------------------------------------

	private void increaseAge() {
		for (NodeDescriptor descriptor : fView) {
			descriptor.increaseAge();
		}
	}
	
	// ----------------------------------------------------------------------

	private CyclonMessage pendingMessage() {
		switch (fState) {
		case REPLY_SENT:
			return fReplyMsg;
		case REQUEST_SENT:
			return fRequestMsg;
		default:
			throw new IllegalStateException("Internal error: no pending messages to return.");
		}
	}

	// ----------------------------------------------------------------------
	
	private void changeState(ProtocolState next) {
		fState = fState.transition(next);
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
