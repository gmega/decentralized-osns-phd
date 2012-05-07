package it.unitn.disi.newscasting.internal.forwarding;

import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.epidemics.IPushContentExchangeStrategy;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.Set;

import peersim.config.IResolver;
import peersim.core.Linkable;
import peersim.core.Node;

import com.google.common.collect.HashMultimap;

/**
 * {@link HistoryForwarding} is a flooding protocol which supports propagation
 * of message histories as a way of reducing the number of duplicates. Note that
 * this implementation does not track histories, but rather provides a
 * collection of protected methods (history***) which should be overridden by
 * subclasses to implement the desired behavior.
 * 
 * @author giuliano
 */
public class HistoryForwarding implements IPushContentExchangeStrategy,
		ISelectionFilter, IEventObserver {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	public static final String PAR_CHUNK_SIZE = "chunk_size";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------
	/**
	 * ID for the {@link IApplicationInterface} implementor (the social newscast
	 * application).
	 */
	protected final int fAdaptableId;

	/**
	 * Protocol ID (not {@link Linkable} index) for the social network
	 * {@link Linkable}.
	 */
	protected final int fSocialNetworkId;

	/**
	 * Number of messages to be sent to a peer, per round.
	 */
	protected final int fChunkSize;

	private IGossipMessage fScheduled;

	private ActivityStatus fStatus;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------
	/**
	 * We need to keep track of which nodes still have pending messages to
	 * receive, as we need to know what to send when we pick a node.
	 */
	private HashMultimap<Node, IGossipMessage> fPending = HashMultimap.create();

	public HistoryForwarding(int adaptableId, int socialNetworkId,
			IResolver resolver, String prefix) {
		this(adaptableId, socialNetworkId, resolver.getInt(prefix,
				PAR_CHUNK_SIZE));
	}

	public HistoryForwarding(int adaptableId, int socialNetworkId, int chunkSize) {
		fAdaptableId = adaptableId;
		fSocialNetworkId = socialNetworkId;
		fChunkSize = chunkSize;
		recomputeStatus();
	}

	// ----------------------------------------------------------------------
	// IContentExchangeStrategy interface.
	// ----------------------------------------------------------------------

	public boolean doExchange(SNNode source, SNNode peer) {
		if (!canSelect(source, peer)) {
			throw new IllegalArgumentException("Illegal peer selected.");
		}

		if (fScheduled == null) {
			throw new IllegalStateException("Update hasn't been scheduled.");
		}

		if (!fwTableDelete(peer, fScheduled)) {
			throw new IllegalStateException("Scheduled message disappered "
					+ "before it could be sent (concurrent modification?).");
		}
		
		/** Picks a message to send to the peer. **/
		Object history = historyGetCreate(source, fScheduled);

		/** Sends message + history to the peer. **/
		Object feedback = diffusionObject(peer).receiveMessage(source, peer,
				fScheduled, history);

		// Incorporates the receiver into our history.
		historyAdd(history, peer);

		/** Incorporates feedback information, if there was feedback. */
		if (feedback != null) {
			mergeHistories(source, peer, fScheduled, history, feedback);
		}
		
		// Clears the scheduled message.
		fScheduled = null;

		return true;
	}
	
	// ----------------------------------------------------------------------

	@Override
	public IGossipMessage scheduleNext(Node source, ISelectionFilter filter) {
		// This algorithm basically tries to find any update for which there
		// are destinations allowed by the selection filter. Smarter algorithms
		// might try to find the peer with the most pending messages.
		IGossipMessage scheduled = null;
		Set<Node> candidates = fPending.keySet();
		for (Node candidate : candidates) {
			if (filter.canSelect(source, candidate)) {
				scheduled = fPending.get(candidate).iterator().next();
				break;
			}
		}
		
		fScheduled = scheduled;
		return scheduled;
	}

	// ----------------------------------------------------------------------

	public ActivityStatus status() {
		return fStatus;
	}

	// ----------------------------------------------------------------------

	@Override
	public void clear(Node source) {
		fwTableClear();
	}

	// ----------------------------------------------------------------------
	// IEventObserver interface.
	// ----------------------------------------------------------------------

	/**
	 * An event has been delivered to the application <b>by another update
	 * exchange strategy</b>. We need to add those to our forwarding queues.
	 */
	@Override
	public void delivered(SNNode sender, SNNode receiver,
			IGossipMessage message, boolean duplicate) {

		// Not duplicate, we forward it.
		if (!duplicate) {
			this.addPending(sender, receiver, message, null);
		}

		// Otherwise tries to merge the history object "piggybacked" with the
		// message, if any.
		else {
			Object history = diffusionObject(sender).historyGet(message);
			// If it's null, nevermind.
			if (history != null) {
				Object ourHistory = diffusionObject(receiver).historyGetCreate(
						receiver, message);
				this.mergeHistories(receiver, sender, message, ourHistory,
						history);
			}
		}
	}

	// ----------------------------------------------------------------------

	@Override
	public void localDelivered(IGossipMessage message) {
		addPending(message.originator(), message.originator(), message, null);
	}

	// ----------------------------------------------------------------------
	// ISelectionFilter interface.
	// ----------------------------------------------------------------------

	public boolean canSelect(Node source, Node candidate) {
		/** Only allows nodes with pending messages to be selected. **/
		return fPending.containsKey(candidate);
	}

	// ----------------------------------------------------------------------

	public Node selected(Node source, Node peer) {
		return peer;
	}

	// ----------------------------------------------------------------------
	// Private helper methods.
	// ----------------------------------------------------------------------

	/**
	 * This method is called when an attempt to send a message to a neighbor
	 * results in this neighbor telling us that it was a duplicate, and then
	 * providing us with extra history data to help us avoid further duplicates.
	 * 
	 * This method is rather similar to
	 * {@link #addPending(Node, Node, IGossipMessage, Object)}.
	 */
	private void mergeHistories(Node ourNode, Node feedbackNode,
			IGossipMessage message, Object ourHistory, Object foreignHistory) {
		Linkable ourSn = (Linkable) ourNode.getProtocol(fSocialNetworkId);

		// First, merges the neighbor's history into ours.
		historyMerge(ourHistory, foreignHistory);

		/**
		 * Now, we need to: <BR>
		 * 1 - determine, from the new information received, which neighbors are
		 * known to have already received the message; <BR>
		 * 2 - cancel previously scheduled forwardings of these messages to
		 * these neighbors (if there were any scheduled).
		 */
		for (int i = 0; i < ourSn.degree(); i++) {
			Node neighbor = ourSn.getNeighbor(i);

			// Neighbor not in the network yet.
			if (neighbor == null) {
				continue;
			}
			// Checks if the neighbor is a destination for the message, AND
			// it is contained in the history.
			else if (message.isDestination(neighbor)
					&& historyContains(ourHistory, neighbor)) {
				// Cancels the scheduling. Note that this might not result
				// in a real removal.
				fwTableDelete(neighbor, message);
			}
		}
	}

	// ----------------------------------------------------------------------

	/**
	 * Method called by another {@link HistoryForwarding} instance to tell it to
	 * receive a message. <BR>
	 * <BR>
	 * This makes no modifications to the sender's history.
	 */
	private Object receiveMessage(SNNode sender, SNNode receiver,
			IGossipMessage message, Object history) {

		IApplicationInterface app = (IApplicationInterface) getApplication(receiver);
		Object ourHistory = null;

		/**
		 * Hands the message over to the application layer. If the message isn't
		 * a duplicate, we need to forward it as well.
		 */
		if (app.deliver(sender, receiver, message, this)) {
			addPending(sender, receiver, message, history);
		}
		/**
		 * Message is a duplicate. Tries to return or learn something useful.
		 */
		else {
			ourHistory = historyGetCreate(receiver, message);
			mergeHistories(receiver, sender, message, ourHistory, history);
		}

		// Note that if the history has been just created then we're returning
		// useless stuff, but for the sake of cleanliness that's ok.
		return ourHistory;
	}

	// ----------------------------------------------------------------------

	/**
	 * Method called whenever a <b>new</b> message, which needs to be forwarded,
	 * is received <b>for the first time</b> by the application.
	 */
	private void addPending(Node sender, Node receiver, IGossipMessage message,
			Object history) {

		/** We need our social network, and the message itself. **/
		Linkable ourSn = (Linkable) receiver.getProtocol(fSocialNetworkId);

		// Initializes the history for this message from what we just received.
		Object ourHistory = (history == null) ? historyCreate(message)
				: historyClone(message, history);

		// Add ourselves to the history.
		historyAdd(ourHistory, receiver);

		/**
		 * Determines to which of our neighbors we need to forward this message
		 * to. This is determined by the set:
		 * 
		 * ({message destinations} ∩ f(receiver)) - (history ∪ {sender} ∪ <false
		 * positives>))
		 */
		for (int i = 0; i < ourSn.degree(); i++) {
			Node neighbor = ourSn.getNeighbor(i);

			// Neighbor not in the network yet.
			if (neighbor == null) {
				continue;
			}

			if (shouldForward(sender, neighbor, message, ourHistory)) {
				fwTableAdd(neighbor, message);
			}
		}
	}

	// ----------------------------------------------------------------------

	/**
	 * Checks that:<BR>
	 * <ol>
	 * <li>the neighbor is a destination for the message;</li>
	 * <li>the neighbor is not the sender;</li>
	 * <li>the neighbor is not the originator of the message;</li>
	 * <li>the neighbor isn't already in the message history.</li>
	 * 
	 * Note that (2) might be redundant, but the idea is that we never send a
	 * message back to the sender, no matter what the underlying history
	 * implementation does.
	 **/
	private boolean shouldForward(Node sender, Node neighbor,
			IGossipMessage message, Object ourHistory) {
		return message.isDestination(neighbor) && !sender.equals(neighbor)
				&& !historyContains(ourHistory, neighbor)
				&& !message.originator().equals(neighbor);
	}

	// ----------------------------------------------------------------------
	// Read/Write handling of the forwarding table.
	// ----------------------------------------------------------------------

	private void fwTableAdd(Node destination, IGossipMessage message) {
		fPending.put(destination, message);
		recomputeStatus();
	}

	// ----------------------------------------------------------------------

	private boolean fwTableDelete(Node destination, IGossipMessage message) {
		boolean result = fPending.remove(destination, message);
		recomputeStatus();
		return result;
	}

	// ----------------------------------------------------------------------

	private void fwTableClear() {
		fPending.clear();
		recomputeStatus();
	}

	// ----------------------------------------------------------------------

	private void recomputeStatus() {
		fStatus = fPending.size() > 0 ? ActivityStatus.ACTIVE
				: ActivityStatus.QUIESCENT;
	}

	// ----------------------------------------------------------------------
	// Fancy accessors.
	// ----------------------------------------------------------------------

	private IProtocolSet getApplication(Node peer) {
		return (IProtocolSet) peer.getProtocol(fAdaptableId);
	}

	// ----------------------------------------------------------------------

	private HistoryForwarding diffusionObject(Node peer) {
		return getApplication(peer).getStrategy(HistoryForwarding.class);
	}

	// ----------------------------------------------------------------------
	// Monitoring.
	// ----------------------------------------------------------------------

	public int queueSize() {
		return fPending.size();
	}

	// ----------------------------------------------------------------------
	// Methods for manipulating histories. Protected ones are to be overrided
	// by subclasses.
	// ----------------------------------------------------------------------

	/**
	 * Retrieves a history object using {@link #historyGet(IGossipMessage)},
	 * creating a new one and adding the current node if it returns null.
	 */
	private Object historyGetCreate(SNNode current, IGossipMessage message) {
		Object history = historyGet(message);
		if (history == null) {
			history = historyCreate(message);
			/** Adds ourselves to the history. **/
			historyAdd(history, current);
		}
		return history;
	}

	// ----------------------------------------------------------------------

	/**
	 * @return the history object associated with a {@link IGossipMessage}, or
	 *         <code>null</code> if the object cannot be returned (either
	 *         because this node has never seen the message, or perhaps because
	 *         the history object has been evicted from an internal cache).
	 */
	protected Object historyGet(IGossipMessage message) {
		return null;
	}

	// ----------------------------------------------------------------------

	/**
	 * Adds a node into a history object.
	 * 
	 * @param history
	 *            the history object to which the node is to be added.
	 * @param node
	 *            the {@link Node} to be added into the history.
	 */
	protected void historyAdd(Object history, Node node) {
	}

	// ----------------------------------------------------------------------

	/**
	 * @return <code>true</code> if the history object contains the specified
	 *         {@link Node}, or <code>false</code> otherwise. If the history
	 *         object is <code>null</code>, then this method always returns
	 *         <code>false</code>.
	 */
	protected boolean historyContains(Object history, Node element) {
		return false;
	}

	// ----------------------------------------------------------------------

	/**
	 * Merges one history object into another.
	 * 
	 * @param merged
	 *            the history object into which results are merged.
	 * 
	 * @param mergee
	 *            the history object to be merged into the other. This object
	 *            will not be changed.
	 */
	protected void historyMerge(Object merged, Object mergee) {
	}

	// ----------------------------------------------------------------------

	/**
	 * Clones a history object. Subsequent calls to
	 * {@link #historyGet(IGossipMessage)} should return this object.
	 */
	protected Object historyClone(IGossipMessage message, Object history) {
		return null;
	}

	// ----------------------------------------------------------------------

	/**
	 * Creates a history object for a {@link IGossipMessage}. Subsequent calls
	 * to {@link #historyGet(IGossipMessage)} should return this object.
	 */
	protected Object historyCreate(IGossipMessage message) {
		return null;
	}

	// ----------------------------------------------------------------------
}
