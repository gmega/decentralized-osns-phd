package it.unitn.disi.newscasting.internal;

import static it.unitn.disi.newscasting.NewscastEvents.DELIVER_SINGLE_TWEET;
import static it.unitn.disi.newscasting.NewscastEvents.DUPLICATE_TWEET;
import static it.unitn.disi.newscasting.NewscastEvents.EXCHANGE_DIGESTS;
import static it.unitn.disi.newscasting.NewscastEvents.TWEETED;
import it.unitn.disi.newscasting.BinaryCompositeFilter;
import it.unitn.disi.newscasting.EventRegistry;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.ISelectionFilter;
import it.unitn.disi.newscasting.NewscastEvents;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.LogManager;
import it.unitn.disi.utils.peersim.FallThroughReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Base implementation for the social newscasting service.
 * 
 * @author giuliano
 */
@AutoConfig
public class SocialNewscastingService implements CDProtocol, ICoreInterface, IApplicationInterface {

	// ----------------------------------------------------------------------
	// Logging constants and shared structures.
	// ----------------------------------------------------------------------

	/**
	 * String identifier for the log in which events are to be written to.
	 */
	private static final String TWEET_LOG = "tweets";

	private static final EventCodec fCodec = new EventCodec(Byte.class,
			NewscastEvents.values());

	protected static final byte[] fBuffer = new byte[NewscastEvents.set
			.sizeof(NewscastEvents.set.getLargest())];

	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------

	/**
	 * ID of the {@link Linkable} containing the social neighborhood.
	 */
	@Attribute("social_neighborhood")
	private int fSocialNetworkID;
	
	/**
	 * Our own protocol ID.
	 */
	private int fProtocolID;

	/**
	 * If set to true, causes events to be logged into text form as well into
	 * the standard error output.
	 */
	@Attribute("verbose")
	private boolean fVerbose;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	/** Storage for events. */
	private IWritableEventStorage fStorage;

	/** Tweet sequence number. */
	private int fSeqNumber = 0;

	/** Calls received in total. */
	private int fContacts = 0;
	
	/** Messages pending delivery to this app. */
	private int fPending;
	
	// ----------------------------------------------------------------------
	// Internal instances (so that the class doesn't expose these interfaces).
	// ----------------------------------------------------------------------

	private MergeObserverImpl fObserver;

	// ----------------------------------------------------------------------
	// Exchange strategies configuration and communication. 
	// ----------------------------------------------------------------------

	private BroadcastBus fChannel;

	/**
	 * The configured exchange strategies.
	 */
	private ArrayList<StrategyEntry> fStrategies;

	/**
	 * Configured adapters.
	 */
	private Map<AdapterKey, Object> fAdapters;

	// ----------------------------------------------------------------------
	// Logging. 
	// ----------------------------------------------------------------------

	/**
	 * Log manager instance.
	 */
	private LogManager fManager;
	
	// ----------------------------------------------------------------------
	// Misc. 
	// ----------------------------------------------------------------------
	
	private IMessageVisibility fVisibility;

	private String fPrefix;

	// ----------------------------------------------------------------------

	public SocialNewscastingService(@Attribute(Attribute.PREFIX) String prefix)
			throws IOException {
		// Inits the log manager.
		fPrefix = prefix;
		fManager = LogManager.getInstance();
		fManager.add(prefix);
		fProtocolID = this.resolveProtocolId(fPrefix);

		// Configures the peer selection and update exchange strategies.
		this.configure();
	}

	// ----------------------------------------------------------------------
	
	private void configure() {
		this.flushState();
		this.newConfigurator(fPrefix).configure(this,
				fProtocolID, fSocialNetworkID);
	}
	
	// ----------------------------------------------------------------------

	private void flushState() {
		fObserver = new MergeObserverImpl();
		fAdapters = new HashMap<AdapterKey, Object>();
		fStrategies = new ArrayList<StrategyEntry>();
		fChannel = new BroadcastBus();
		fVisibility = new DefaultVisibility(fSocialNetworkID);
	}
	
	// ----------------------------------------------------------------------
	
	private int resolveProtocolId(String prefix) {
		int idx = prefix.lastIndexOf('.');
		idx = (idx == -1) ? 0 : idx;
		String name = prefix.substring(idx + 1);
		return Configuration.lookupPid(name);
	}
	
	// ----------------------------------------------------------------------
	// Configuration callbacks. 
	// ----------------------------------------------------------------------

	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> intf,
			Class<? extends Object> key) {
		return (T) fAdapters.get(new AdapterKey(intf, key));
	}
	
	// ----------------------------------------------------------------------

	public void addSubscriber(IEventObserver observer) {
		fChannel.addSubscriber(observer);
	}
	
	// ----------------------------------------------------------------------

	protected void setAdapter(Class<? extends Object> intf,
			Class<? extends Object> key, Object adapted) {
		fAdapters.put(new AdapterKey(intf, key), adapted);
	}

	// ----------------------------------------------------------------------

	protected void addStrategy(IContentExchangeStrategy strategy,
			IReference<IPeerSelector> selector,
			IReference<ISelectionFilter> filter, double probability) {
		fStrategies.add(new StrategyEntry(strategy, selector, new BinaryCompositeFilter(
				new FallThroughReference<ISelectionFilter>(
						ISelectionFilter.UP_FILTER), filter), probability));
	}

	// ----------------------------------------------------------------------
	
	// DON'T CALL THIS, EVER. It's the product of a flaw in an evolving design.
	protected IMergeObserver internalObserver() {
		return fObserver;
	}
	
	// ----------------------------------------------------------------------
	
	private IApplicationConfigurator newConfigurator(String prefix) {
		return new NewscastAppConfigurator(prefix);
	}
	
	// ----------------------------------------------------------------------
	
	private Linkable socialNetwork(Node source) {
		return (Linkable) source.getProtocol(fSocialNetworkID);
	}

	// ----------------------------------------------------------------------
	// CDProtocol interface
	// ----------------------------------------------------------------------

	public void nextCycle(Node ourNode, int protocolID) {
		/**
		 * Runs the configured exchange strategies, with the configured peer
		 * selectors.
		 */
		for (int i = 0; i < fStrategies.size(); i++) {
			if (!runStrategy(i)) {
				continue;
			}
			
			StrategyEntry entry = fStrategies.get(i);
			
			Node peer = selectPeer(ourNode, entry.selector.get(ourNode),
					entry.filter);

			performExchange(ourNode, peer, entry.strategy);
		}
	}
	
	// ----------------------------------------------------------------------
	
	private Node selectPeer(Node ourNode, IPeerSelector selector,
			BinaryCompositeFilter filter) {

		// Selects a peer using the preconfigured filter.
		filter.bind(ourNode);
		Node peer;
		if (selector.supportsFiltering()) {
			peer = selector.selectPeer(ourNode, filter);
		} else {
			peer = selector.selectPeer(ourNode);
		}
		filter.clear();
		return peer;
	}
	
	// ----------------------------------------------------------------------
	
	private void performExchange(Node ourNode, Node peer, IContentExchangeStrategy strategy) {
		// Null peer means no game.
		if (peer == null) {
			return;
		}
		
		// Gets the throttling. It cannot be zero, as zero throttling
		// means the peer shouldn't have passed the selection filter.
		int runs = strategy.throttling(peer);
		if (runs <= 0) {
			throw new IllegalStateException(
					"Negative or zero throttling are not allowed.");
		}
		
		// Performs the actual exchanges.
		for (int j = 0; j < runs; j++) {
			if (!strategy.doExchange(ourNode, peer)) {
				break;
			}
		}		
	}

	// ----------------------------------------------------------------------

	private boolean runStrategy(int i) {
		if (CommonState.r.nextDouble() < fStrategies.get(i).probability) {
			return true;
		}

		return false;
	}

	// ----------------------------------------------------------------------

	private void countPending() {
		fPending++;
	}

	// ----------------------------------------------------------------------
	// IApplication interface. 
	// ----------------------------------------------------------------------
	
	public IEventStorage storage() {
		return fStorage;
	}
	
	// ----------------------------------------------------------------------

	public void postToFriends() {
		this.replyToPost(null);		
	}
	
	// ----------------------------------------------------------------------
	
	public void replyToPost(Tweet replyTo) {
		// As per the contract.
		Node us = CommonState.getNode();
		Tweet tweet = new Tweet(us, ++fSeqNumber, fVisibility, replyTo);
		fStorage.add(tweet);

		// Notifies everyone.
		fObserver.tweeted(tweet);
	}
	
	// ----------------------------------------------------------------------
	// ICoreInterface
	// ----------------------------------------------------------------------

	public boolean receiveTweet(Node sender, Node ours,
			Tweet tweet, IEventObserver broadcaster) {
		
		// This is kind of an ugly hack...
		fChannel.beginBroadcast(broadcaster);		
		
		boolean added = fStorage.add(tweet);
		fObserver.eventDelivered(sender, ours, tweet, !added);
		
		return added;
	}
	
	// ----------------------------------------------------------------------
	
	public int pendingReceives() {
		return fPending;
	}
	
	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		// Cloning here is tricky because of loose coupling. Therefore, calling
		// configure is the best way out, but it won't produce clones: only
		// fresh instances.
		try {
			SocialNewscastingService cloned = (SocialNewscastingService) super.clone();
			cloned.fStorage = (IWritableEventStorage) this.fStorage.clone();
			cloned.configure();
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Should never happen...
		}
	}

	// ----------------------------------------------------------------------
	// Logging methods.
	// ----------------------------------------------------------------------

	private void log(int len) {
		fManager.logWrite(TWEET_LOG, fBuffer, len);
	}

	// ----------------------------------------------------------------------
	// IMergeObserver interface.
	// ----------------------------------------------------------------------
	
	private class MergeObserverImpl implements IMergeObserver {

		// ----------------------------------------------------------------------

		public void tweeted(Tweet tweet) {
			// Sanity check.
			if (CommonState.getNode() != tweet.poster) {
				throw new IllegalStateException(
						"Node can only tweet in its turn.");
			}
			
			log(fBuffer, TWEETED.magicNumber(),	// event type
					tweet.poster.getID(),	// tweeting node (us)
					tweet.sequenceNumber,		// sequence number
					CommonState.getTime());		// simulation time
			
			// Declares to our neighbors that they have an extra pending message.
			Linkable sn = socialNetwork(tweet.poster);
			int degree = sn.degree();
			for (int i = 0; i < degree; i++) {
				Node owner = sn.getNeighbor(i);
				((SocialNewscastingService) owner.getProtocol(fProtocolID)).countPending();
			}
			
			fChannel.tweeted(tweet);
		}

		// ----------------------------------------------------------------------

		/**
		 * Registers the reception of a range of tweet events.
		 */
		public void eventDelivered(Node sending, Node receiving, Tweet tweet,
				boolean duplicate) {

			// Sanity check.
			if (tweet.poster.equals(receiving)) {
				throw new InternalError();
			}
			
			if(!duplicate) {
				log(fBuffer, DELIVER_SINGLE_TWEET.magicNumber(),// event type
						tweet.poster.getID(), 				// node owning the tweet
						sending.getID(),						// the sending node.
						receiving.getID(),						// node receiving the tweet
						tweet.sequenceNumber,					// sequence number of the tweet
						CommonState.getTime());					// simulation time

				// One less pending tweet to receive.
				fPending--;
			} 
			
			else {
				log(fBuffer, DUPLICATE_TWEET.magicNumber(),// event type
						tweet.poster.getID(), 				// node owning the tweet
						sending.getID(),						// the sending node.
						receiving.getID(),						// node receiving the tweet
						tweet.sequenceNumber,					// sequence number of the tweet
						CommonState.getTime());					// simulation time
			}

			// Passes the event forward.
			fChannel.eventDelivered(sending, receiving, tweet, duplicate);
		}
		
		// ----------------------------------------------------------------------

		public void sendDigest(Node sender, Node receiver, Node owner,
				List<Integer> holes) {
			// When digests are exchanged, they flow from the node
			// initiating the anti-entropy exchange to the pairing node.
			// (The initiating node tells the pair what it doesn't have).
			log(fBuffer, EXCHANGE_DIGESTS
					.magicNumber(), // Event type
					sender.getID(), // ID of the digest sender.
					receiver.getID(), // ID of digest receiver.
					holes.size(), // Number of items in the digest.
					CommonState.getTime()); // Simulation time.
			
			fChannel.sendDigest(sender, receiver, owner, holes);
		}

		// ----------------------------------------------------------------------
		
		private void log(byte[] buf, Number...event) {
			if (fVerbose) {
				System.out.println(fCodec.toString(event));
			}

			int len = fCodec.encodeEvent(buf, 0, event);
			SocialNewscastingService.this.log(len);
		}
	};

	// ----------------------------------------------------------------------
	// Debugging methods
	// ----------------------------------------------------------------------

	public int contacts() {
		return fContacts;
	}
	
	// ----------------------------------------------------------------------

	public void resetCounters() {
		fPending = fContacts = 0;
	}

	// ----------------------------------------------------------------------

}

// ----------------------------------------------------------------------

class AdapterKey {
	public final Class<? extends Object> intf;
	public final Class<? extends Object> key;

	public AdapterKey(Class<? extends Object> intf, Class<? extends Object> key) {
		this.intf = intf;
		this.key = key;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof AdapterKey)) {
			return false;
		}

		AdapterKey ak = (AdapterKey) obj;
		return ak.intf == this.intf && ak.key == this.key;
	}

	public int hashCode() {
		int result = 53;
		result = 37 * result + intf.hashCode();
		if (key != null) {
			result = 37 * result + key.hashCode();
		}

		return result;
	}
}

class StrategyEntry {

	public final IContentExchangeStrategy strategy;
	public final BinaryCompositeFilter filter;
	public final IReference<IPeerSelector> selector;

	public final double probability;

	public StrategyEntry(IContentExchangeStrategy strategy,
			IReference<IPeerSelector> selector,
			BinaryCompositeFilter filter, double probability) {
		this.strategy = strategy;
		this.selector = selector;
		this.filter = filter;
		this.probability = probability;
	}
}
