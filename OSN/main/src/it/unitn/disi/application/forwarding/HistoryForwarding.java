package it.unitn.disi.application.forwarding;

import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.HashMultimap;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.IAdaptable;
import it.unitn.disi.application.IApplication;
import it.unitn.disi.application.Tweet;
import it.unitn.disi.application.interfaces.IContentExchangeStrategy;
import it.unitn.disi.application.interfaces.IEventObserver;
import it.unitn.disi.application.interfaces.ISelectionFilter;

/**
 * {@link HistoryForwarding} is a flooding protocol which supports propagation
 * of message histories as a way of reducing the number of duplicates. Note that
 * this implementation does not track histories, but rather provides a
 * collection of protected methods (history***) which should be overridden by
 * subclasses to implement the desired behavior.
 * 
 * @author giuliano
 */
public class HistoryForwarding implements IContentExchangeStrategy, ISelectionFilter, IEventObserver {
	
	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	private static final String PAR_CHUNK_SIZE = "chunk_size";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------
	/**
	 * ID for the {@link IAdaptable} implementor (the social newscast application).
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
	
	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------
	/**
	 * We need to keep track of which nodes still have pending messages to
	 * receive, as we need to know what to send when we pick a node.
	 */
	private HashMultimap<Node, Tweet> fPending = HashMultimap.create();
	
	public HistoryForwarding(int adaptableId, int socialNetworkId, String prefix) {
		this(adaptableId, socialNetworkId, 
				Configuration.getInt(prefix + "." + PAR_CHUNK_SIZE));
	}

	public HistoryForwarding(int adaptableId, int socialNetworkId,
			int chunkSize) {
		fAdaptableId = adaptableId;
		fSocialNetworkId = socialNetworkId;
		fChunkSize = chunkSize;
	}
	
	// ----------------------------------------------------------------------
	// IContentExchangeStrategy interface.
	// ----------------------------------------------------------------------

	public boolean doExchange(Node source, Node peer) {
		if(!canSelect(peer)) {
			throw new IllegalArgumentException("Illegal peer selected.");
		}

		/** Picks a message to send to the peer. **/
		Tweet message = removeAny(peer);
		Object history = historyGet(message);
		if (history == null) {
			history = historyCreate(message);
			/** Adds ourselves to the history. **/
			historyAdd(history, source);
		}
				
		/** Sends message + history to the peer. **/
		Object feedback = diffusionObject(peer).receiveMessage(
				source, peer, message, history);
		
		// Incorporates the receiver into our history.
		historyAdd(history, peer);

		/** Incorporates feedback information, if there was feedback. */
		if (feedback != null) {
			mergeHistories(source, peer, message, history, feedback);
		}
		
		return true;
	}
	
	public int throttling(Node target) {
		Set<Tweet> pendings = fPending.get(target);
		if (pendings == null) {
			return 0;
		}
		
		return Math.min(fChunkSize, pendings.size());
	}
	
	// ----------------------------------------------------------------------
	// IEventObserver interface.
	// ----------------------------------------------------------------------

	/**
	 * An event (range) has been delivered to the application by another update
	 * exchange strategy. We need to add those to our forwarding queues.
	 * 
	 * Note that "delivered" means that these messages are new to the
	 * application, so we call
	 * {@link #addPending(Node, Node, Tweet, Object)} in the inner loop.
	 */
	public void eventDelivered(Node sender, Node receiver, Node owner,
			int start, int end) {
		Linkable ourSn = (Linkable) receiver.getProtocol(fSocialNetworkId);
		
		for (int i = (start == -1 ? end : start); i <= end; i++) {
			Tweet tweet = new Tweet(owner, i);
			for (int j = 0; j < ourSn.degree(); j++) {
				Node neighbor = ourSn.getNeighbor(i);
				if (neighbor != null) {
					this.addPending(sender, receiver, tweet, null);
				}
			}
		}
	}
	
	// ----------------------------------------------------------------------

	public int queueSize() {
		return fPending.size();
	}
	
	// ----------------------------------------------------------------------

	public void tweeted(Node owner, int sequenceNumber) {
		addPending(owner, owner, new Tweet(owner, sequenceNumber), null);
	}
	
	// ----------------------------------------------------------------------

	public void duplicateReceived(Node sender, Node receiver, Node owner,
			int start, int end) {
		// Do nothing.
	}
	
	// ----------------------------------------------------------------------
	// ISelectionFilter interface.
	// ----------------------------------------------------------------------
	
	public boolean canSelect(Node candidate) {
		/** Only allows nodes with pending messages to be selected. **/
		return fPending.containsKey(candidate);
	}
	
	// ----------------------------------------------------------------------
	
	public Node selected(Node node) { return node; }

	// ----------------------------------------------------------------------
	// Private helper methods.
	// ----------------------------------------------------------------------
	
	/**
	 * This method is called when an attempt to send a message to a neighbor
	 * results in this neighbor telling us that it was a duplicate, and then
	 * providing us with extra history data to help us avoid further duplicates.
	 * 
	 * This method is rather similar to {@link #addPending(Node, Node, Tweet, Object)}. 
	 */
	private void mergeHistories(Node ourNode, Node feedbackNode, Tweet tweet,
			Object ourHistory, Object feedbackHistory) {
		Linkable ourSn = (Linkable) ourNode.getProtocol(fSocialNetworkId);
		Linkable originator = (Linkable) tweet.fNode.getProtocol(fSocialNetworkId);
		
		// First, merges the neighbor's history into ours.
		historyMerge(ourHistory, feedbackHistory);

		/** Now, we need to:
		 * 1 - determine, from the new information received, which neighbors 
		 *     are known to have already received the message;
		 * 2 - remove the tweet from their send queues (if it's there).
		 */
		for (int i = 0; i < ourSn.degree(); i++) {
			Node neighbor = ourSn.getNeighbor(i);
			
			// Neighbor not in the network yet.
			if (neighbor == null) {
				continue;
			}
			// Checks if the neighbor is shared with the originator, AND
			// it is in the history.
			else if (originator.contains(neighbor)
					&& historyContains(ourHistory, neighbor)) {
				// Note that this might not result in a real removal.
				fPending.remove(neighbor, tweet);
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
	private Object receiveMessage(Node sender, Node receiver,
			Tweet tweet, Object history) {

		IApplication app = (IApplication) getApplication(receiver);
		Object ourHistory = null;

		/** Hands the message over to the application layer. If the message isn't
		 * a duplicate, we need to forward it as well. */
		if (app.receiveTweet(sender, receiver, tweet, this)) {
			addPending(sender, receiver, tweet, history);
		}
		/** Message is a duplicate. Tries to return something useful. */
		else {
			ourHistory = historyGet(tweet);
			// Incorporates the history we received into ours.
			if (ourHistory != null) {
				historyMerge(ourHistory, history);
			} 
			// If the message was a duplicate AND we have no history of it, 
			// it means the underlying history tracker has disposed of it 
			// somehow. Just re-create it from what we received.
			else {
				historyClone(tweet, history);
			}
		}
		
		return ourHistory;
	}
	
	// ----------------------------------------------------------------------

	/**
	 * Method called whenever a <b>new</b> message, which needs to be forwarded,
	 * is received. Parameter names are self-explanatory.  
	 * <BR>
	 * When this method is called because the receiver is <i>tweeting</i>, then
	 * the following must hold:
	 * <BR>
	 * <ol>
	 * <li> <code>sender == receiver</code> must yield true; </li>
	 * <li> history must be <code>null</code>. </li>
	 * </ol>
	 * <BR>
	 * This method already puts the local history in the cache.
	 * 
	 */
	private void addPending(Node sender, Node receiver, Tweet tweet, Object history) {
		/** We need: 
		 * 1 - our social network;
		 * 2 - the social network of the originating node. */
		Linkable ourSn = (Linkable) receiver.getProtocol(fSocialNetworkId);
		Linkable originator = (Linkable) tweet.fNode.getProtocol(fSocialNetworkId);
		
		// Initializes the history for this message from what we just received.
		Object ourHistory = (history == null) ? historyCreate(tweet)
				: historyClone(tweet, history);
		
		// Add ourselves to the history.
		historyAdd(ourHistory, receiver);
		
		/**
		 * Determines to which of our neighbors we need to forward this message to.
		 * This is determined by the set: 
		 * 
		 * 		(f(originator) ∩ f(receiver)) - (history ∪ <false positives>)
		 */
		for (int i = 0; i < ourSn.degree(); i++) {
			Node neighbor = ourSn.getNeighbor(i);
			
			// Neighbor not in the network yet.
			if (neighbor == null) {
				continue;
			} 
			
			// Node is tweeting, so we just need to forward it
			// to everyone. 
			else if ((history == null) && (sender == receiver)) {
				// Sanity check.
				if (tweet.fNode != sender) {
					throw new IllegalArgumentException();
				}
				
				fPending.put(neighbor, tweet);
			}
			
			// Checks if this neighbor is friends with the originator, since we
			// are not interested in people who aren't his friends, AND checks
			// if the node is not known to have already received the message.
			else if (originator.contains(neighbor) && 
					!historyContains(history, neighbor)) {
					// Not in history, need to forward it. 
				fPending.put(neighbor, tweet);
			}
		}
	}
	
	// ----------------------------------------------------------------------

	private Tweet removeAny(Node peer) {
		Set<Tweet> pendings = fPending.get(peer);
		Iterator<Tweet> it = pendings.iterator();
		Tweet toReturn = it.next();
		it.remove();
		return toReturn;
	}
	
	// ----------------------------------------------------------------------
	// Fancy accessors.
	// ----------------------------------------------------------------------
	
	private IApplication getApplication(Node peer) {
		return (IApplication) peer.getProtocol(fAdaptableId);
	}
	
	// ----------------------------------------------------------------------

	private HistoryForwarding diffusionObject(Node peer) {
		return (HistoryForwarding) getApplication(peer).getAdapter(
				HistoryForwarding.class, null);
	}

	// ----------------------------------------------------------------------
	// Methods for manipulating histories. To be overrided by subclasses.
	// ----------------------------------------------------------------------

	/**
	 * @return the history object associated with a {@link Tweet}, or
	 *         <code>null</code> if the object cannot be returned (either
	 *         because this node has never seen the message, or perhaps because
	 *         the history object has been evicted from an internal cache).
	 */
	protected Object historyGet(Tweet tweet) {
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
	protected void historyAdd(Object history, Node node) { }
	
	// ----------------------------------------------------------------------

	/**
	 * @return <code>true</code> if the history object contains the specified
	 *         {@link Node}, or <code>false</code> otherwise.
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
	protected void historyMerge(Object merged, Object mergee) { }

	// ----------------------------------------------------------------------

	/**
	 * Clones a history object. Subsequent calls to {@link #historyGet(Tweet)}
	 * should return this object.
	 */
	protected Object historyClone(Tweet tweet, Object history) {
		return null;
	}
	
	// ----------------------------------------------------------------------

	/**
	 * Creates a history object for a {@link Tweet}. Subsequent calls to
	 * {@link #historyGet(Tweet)} should return this object.
	 */
	protected Object historyCreate(Tweet tweet) {
		return null;
	}
	
	// ----------------------------------------------------------------------
}
