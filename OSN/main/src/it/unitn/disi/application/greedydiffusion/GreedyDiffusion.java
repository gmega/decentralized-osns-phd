package it.unitn.disi.application.greedydiffusion;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.skjegstad.utils.BloomFilter;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.application.ComponentComputationService;
import it.unitn.disi.application.IAdaptable;
import it.unitn.disi.application.IApplication;
import it.unitn.disi.application.Tweet;
import it.unitn.disi.application.interfaces.IContentExchangeStrategy;
import it.unitn.disi.application.interfaces.IEventObserver;
import it.unitn.disi.application.interfaces.ISelectionFilter;

/**
 * Greedy diffusion is a smarter flooding algorithm which tries to keep a tab on
 * duplicates by tracking message histories.
 * 
 * @author giuliano
 */
public class GreedyDiffusion implements IContentExchangeStrategy, ISelectionFilter, IEventObserver {
	
	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	private static final String PAR_CHUNK_SIZE = "chunk_size";
	
	private static final String PAR_WINDOW_SIZE = "window_size";
	
	private static final String PAR_BLOOM_FALSE_POSITIVE = "bloom_false_positive";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------
	/**
	 * ID for the {@link IAdaptable} implementor (the social newscast application).
	 */
	private final int fAdaptableId;

	/**
	 * Protocol ID (not {@link Linkable} index) for the social network
	 * {@link Linkable}.
	 */
	private final int fSocialNetworkId;

	/**
	 * False positive probability for bloom filters.
	 */
	private final double fBFFalsePositive;

	/**
	 * Number of messages to be sent to a peer, per round.
	 */
	private final int fChunkSize;
	
	/**
	 * Size of the history cache. 
	 */
	private final int fWindowSize;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------
	/**
	 * We need to keep track of which nodes still have pending messages to
	 * receive, as we need to know what to send when we pick a node.
	 */
	private HashMultimap<Node, Tweet> fPending = HashMultimap.create();

	// ----------------------------------------------------------------------
	// Tracking statistics.
	// ----------------------------------------------------------------------
	private int fCacheAccesses;

	private int fCacheHits;

	/**
	 * Since we cannot keep histories for every message in memory, we keep a
	 * window.
	 */
	@SuppressWarnings("serial")
	private LinkedHashMap<Tweet, BloomFilter<Long>> fWindow = new LinkedHashMap<Tweet, BloomFilter<Long>>() {
		@Override
		protected boolean removeEldestEntry(
				Map.Entry<Tweet, BloomFilter<Long>> entry) {
			if (size() == fWindowSize) {
				return true;
			}

			return false;
		}
	};
	
	public GreedyDiffusion(int adaptableId, int socialNetworkId, String prefix) {
		this(adaptableId, socialNetworkId, 
				Configuration.getInt(prefix + "." + PAR_CHUNK_SIZE),
				Configuration.getInt(prefix + "." + PAR_WINDOW_SIZE),
				Configuration.getDouble(prefix + "." + PAR_BLOOM_FALSE_POSITIVE));
	}

	public GreedyDiffusion(int adaptableId, int socialNetworkId,
			int chunkSize, int windowSize, double bFFalsePositive) {
		fAdaptableId = adaptableId;
		fSocialNetworkId = socialNetworkId;
		fChunkSize = chunkSize;
		fWindowSize = windowSize;
		fBFFalsePositive = bFFalsePositive;
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
		BloomFilter<Long> history = cacheLookup(message);
		if (history == null) {
			history = initializeHistory(message, source);
			/** Adds ourselves to the history. **/
			history.add(source.getID());
		}

		/** Sends message + history to the peer. **/
		BloomFilter<Long> feedback = diffusionObject(peer).receiveMessage(
				source, peer, message, history);
		
		// Incorporates the receiver into our history.
		history.add(peer.getID());

		/** Incorporates feedback information, if there was feedback. */
		if (feedback != null) {
			this.mergeHistories(source, peer, message, history, feedback);
		}
		
		return true;
	}
	
	// ----------------------------------------------------------------------
	
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
	 * {@link #addPending(Node, Node, Tweet, BloomFilter)} in the inner loop.
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
	 * Method called by another {@link GreedyDiffusion} instance to tell it to
	 * receive a message. <BR>
	 * <BR>
	 * This makes no modifications to the sender's history.
	 */
	private BloomFilter<Long> receiveMessage(Node sender, Node receiver,
			Tweet tweet, BloomFilter<Long> history) {

		IApplication app = (IApplication) getApplication(receiver);
		BloomFilter<Long> ourHistory = null;
		
		/** Hands the message over to the application layer. If the message isn't
		 * a duplicate, we need to forward it as well. */
		if (app.receiveTweet(this, sender, receiver, tweet)) {
			addPending(sender, receiver, tweet, history);
		}
		/** Message is a duplicate. Tries to return something useful. */
		else {
			ourHistory = cacheLookup(tweet);
			// Incorporates the history we received into ours.
			if (ourHistory != null) {
				ourHistory.merge(history);
			} 
			// Or, if the history has been evicted, just cache a copy
			// of whatever we got.
			else {
				cache(tweet, cloneHistory(history));
			}
		}

		return ourHistory;
	}
	
	// ----------------------------------------------------------------------

	/**
	 * Method called whenever a <b>new</b> message, which needs to be forwarded,
	 * is received. Parameter names are self-explanatory.  
	 * 
	 * When this method is called because the receiver is <i>tweeting</i>, then
	 * the following must hold:
	 * 
	 * 1 - sender == receiver must yield true;
	 * 2 - history must be <code>null</code>.
	 * 
	 * This method already puts the local history in the cache.
	 * 
	 */
	private void addPending(Node sender, Node receiver, Tweet tweet, BloomFilter<Long> history) {
		/** We need: 
		 * 1 - our social network;
		 * 2 - the social network of the originating node. */
		Linkable ourSn = (Linkable) receiver.getProtocol(fSocialNetworkId);
		Linkable originator = (Linkable) tweet.fNode.getProtocol(fSocialNetworkId);
		
		/** Clones or initializes our history. */
		BloomFilter<Long> ourHistory = (history == null) ? initializeHistory(
				tweet, receiver) : cloneHistory(history);
		// Add ourselves to the history.
		ourHistory.add(receiver.getID());
		
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
			else if (history == null) {
				fPending.put(neighbor, tweet);
			}
			
			// Checks if this neighbor is friends with the originator, since we
			// are not interested in people who aren't his friends, AND checks
			// if the node is not known to have already received the message.
			else if (originator.contains(neighbor) && 
					!ourHistory.contains(neighbor.getID())) {
					// Not in history, need to forward it. 
				fPending.put(neighbor, tweet);
			}
		}

		cache(tweet, ourHistory);
	}
	
	// ----------------------------------------------------------------------

	/**
	 * This method is called when an attempt to send a message to a neighbor
	 * results in this neighbor telling us that it was a duplicate, and then
	 * providing us with extra history data to help us avoid further duplicates.
	 * 
	 * This method is rather similar to {@link #addPending(Node, Node, Tweet, BloomFilter)}. 
	 */
	private void mergeHistories(Node ourNode, Node feedbackNode, Tweet tweet,
			BloomFilter<Long> ourFilter, BloomFilter<Long> feedbackFilter) {
		Linkable ourSn = (Linkable) ourNode.getProtocol(fSocialNetworkId);
		Linkable originator = (Linkable) tweet.fNode.getProtocol(fSocialNetworkId);
		
		// First, merges the neighbor's history into ours.
		ourFilter.merge(feedbackFilter);

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
					&& ourFilter.contains(neighbor.getID())) {
				// Note that this might not result in a real removal.
				fPending.remove(neighbor, tweet);
			}
		}
	}
	
	// ----------------------------------------------------------------------
	
	public BloomFilter<Long> initializeHistory(Tweet tweet, Node ours) {
		// Determines the size of the neighborhood over which we are disseminating.
		Node central = tweet.fNode;
		Linkable socialNeighborhood = (Linkable) central.getProtocol(fSocialNetworkId);
		int bloomFilterSize = (int) BloomFilter.requiredBitSetSizeFor(fBFFalsePositive, socialNeighborhood.degree());
		
		BloomFilter<Long> bloom = new BloomFilter<Long>(bloomFilterSize, socialNeighborhood.degree());
		return bloom;
	}
	
	// ----------------------------------------------------------------------
	
	public BloomFilter<Long> cloneHistory(BloomFilter<Long> other) {
		BloomFilter<Long> clone = new BloomFilter<Long>(other.size(), other
				.getExpectedNumberOfElements());
		clone.merge(other);
		return clone;
	}
	
	// ----------------------------------------------------------------------

	/**
	 * Looks up the history of a message in the local cache.
	 * 
	 * @param tweet
	 * 
	 * @return a {@link BloomFilter} with the message history, or
	 *         <code>null</code> if it cannot be found.
	 */
	private BloomFilter<Long> cacheLookup(Tweet tweet) {
		fCacheAccesses++;
		BloomFilter<Long> cached = fWindow.get(tweet);
		if (cached != null) {
			fCacheHits++;
		}
		return cached;
	}
	
	// ----------------------------------------------------------------------
	
	private void cache(Tweet tweet, BloomFilter<Long> filter) {
		if (fWindow.containsKey(tweet)) {
			throw new IllegalStateException("Attempt to cache a duplicate tweet.");
		}
		
		fWindow.put(tweet, filter);
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

	private GreedyDiffusion diffusionObject(Node peer) {
		return (GreedyDiffusion) getApplication(peer).getAdapter(
				GreedyDiffusion.class, null);
	}
	
	// ----------------------------------------------------------------------
	// Monitoring methods.
	// ----------------------------------------------------------------------
	
	public int cacheHits() {
		return fCacheHits;
	}
	
	// ----------------------------------------------------------------------
	
	public int cacheReads() {
		return fCacheAccesses;
	}
	
	// ----------------------------------------------------------------------
	
	public double cacheHitRate() {
		if (fCacheAccesses == 0) {
			return 1.0;
		}
		return ((double) fCacheHits) / fCacheAccesses;
	}
}
