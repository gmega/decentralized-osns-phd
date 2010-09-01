package it.unitn.disi.newscasting.internal;

import static it.unitn.disi.newscasting.NewscastEvents.DELIVER_SINGLE_TWEET;
import static it.unitn.disi.newscasting.NewscastEvents.DUPLICATE_TWEET;
import static it.unitn.disi.newscasting.NewscastEvents.EXCHANGE_DIGESTS;
import static it.unitn.disi.newscasting.NewscastEvents.TWEETED;
import it.unitn.disi.newscasting.BinaryCompositeFilter;
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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

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
public class SocialNewscastingService implements CDProtocol, ICoreInterface,
		IApplicationInterface {

	// ----------------------------------------------------------------------
	// Logging constants and shared structures.
	// ----------------------------------------------------------------------

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
	private final int fSocialNetworkID;

	/**
	 * Our own protocol ID.
	 */
	private final int fProtocolID;

	/**
	 * If set to true, causes events to be logged into text form as well into
	 * the standard error output.
	 */
	private final boolean fVerbose;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	/** Storage for events. */
	private IWritableEventStorage fStorage;

	/** Tweet sequence number. */
	private int fSeqNumber = 0;

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
	private HashMap<Class<? extends IContentExchangeStrategy>, IContentExchangeStrategy> fStrategyIndex;
	
	private ArrayList<StrategyEntry> fStrategies;

	// ----------------------------------------------------------------------
	// Logging.
	// ----------------------------------------------------------------------

	/**
	 * Log handle.
	 */
	private OutputStream fTweetLog;

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	private IMessageVisibility fVisibility;

	private String fPrefix;

	// ----------------------------------------------------------------------

	public SocialNewscastingService(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("social_neighborhood") int socialNetworkId,
			@Attribute(value = "verbose", defaultValue = "false") boolean verbose
		) throws IOException {
		
		this(prefix, protocolId(prefix), socialNetworkId, log(prefix),
				configurator(prefix), verbose);
	}
	
	// ----------------------------------------------------------------------
	
	public SocialNewscastingService(String prefix, int protocolID,
			int socialNetworkId, OutputStream log,
			IApplicationConfigurator configurator, boolean verbose) {
		fPrefix = prefix;
		fProtocolID = protocolID;
		fSocialNetworkID = socialNetworkId;
		fVerbose = verbose;
		
		this.configure(configurator);
	}
	
	// ----------------------------------------------------------------------
	
	private void configure(IApplicationConfigurator configurator) {
		this.flushState();
		configurator.configure(this, fProtocolID, fSocialNetworkID);
	}
	
	// ----------------------------------------------------------------------


	private void flushState() {
		fObserver = new MergeObserverImpl();
		fStrategyIndex = new HashMap<Class<? extends IContentExchangeStrategy>, IContentExchangeStrategy>();
		fStrategies = new ArrayList<StrategyEntry>();
		fChannel = new BroadcastBus();
		fVisibility = new DefaultVisibility(fSocialNetworkID);
	}

	// ----------------------------------------------------------------------

	private static int protocolId(String prefix) {
		int idx = prefix.lastIndexOf('.');
		idx = (idx == -1) ? 0 : idx;
		String name = prefix.substring(idx + 1);
		return Configuration.lookupPid(name);
	}
	
	// ----------------------------------------------------------------------
	
	private static OutputStream log(String prefix) throws IOException {
		LogManager manager = LogManager.getInstance();
		return manager.get(manager.addUnique(prefix));
	}
	
	// ----------------------------------------------------------------------

	private static IApplicationConfigurator configurator(String prefix) {
		return new NewscastAppConfigurator(prefix);
	}
	
	// ----------------------------------------------------------------------
	// Configuration callbacks.
	// ----------------------------------------------------------------------

	public void addStrategy(Class<? extends IContentExchangeStrategy> [] keys,
			IContentExchangeStrategy strategy,
			IReference<IPeerSelector> selector,
			IReference<ISelectionFilter> filter, double probability) {

		StrategyEntry entry = new StrategyEntry(strategy, selector,
				new BinaryCompositeFilter(
						new FallThroughReference<ISelectionFilter>(
								ISelectionFilter.UP_FILTER), filter),
				probability);
		
		fStrategies.add(entry);
		
		for (Class<? extends IContentExchangeStrategy> key : keys) {
			fStrategyIndex.put(key, strategy);
		}
	}

	// ----------------------------------------------------------------------
	
	protected void setStorage(IWritableEventStorage storage) {
		fStorage = storage;
	}

	// ----------------------------------------------------------------------

	// DON'T CALL THIS, EVER. It's the product of a flaw in an evolving design.
	// Mainly I'm not sure where anti-entropy sits in our object model.
	protected IMergeObserver internalObserver() {
		return fObserver;
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

	private void performExchange(Node ourNode, Node peer,
			IContentExchangeStrategy strategy) {
		// Null peer means no game.
		if (peer == null) {
			return;
		}

		// Gets the throttling. It cannot be zero, as zero throttling
		// means the peer shouldn't have been selected in the first place.
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

	@Override
	public boolean receiveTweet(Node sender, Node ours, Tweet tweet,
			IEventObserver broadcaster) {

		// This is kind of an ugly hack...
		fChannel.beginBroadcast(broadcaster);

		boolean added = fStorage.add(tweet);
		fObserver.eventDelivered(sender, ours, tweet, !added);

		return added;
	}

	// ----------------------------------------------------------------------

	@Override
	public void addSubscriber(IEventObserver observer) {
		fChannel.addSubscriber(observer);
	}

	// ----------------------------------------------------------------------

	@Override
	public int pendingReceives() {
		return fPending;
	}

	// ----------------------------------------------------------------------

	@Override
	public Collection<Class<? extends IContentExchangeStrategy>> strategies() {
		return Collections.unmodifiableCollection(fStrategyIndex.keySet());
	}

	// ----------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IContentExchangeStrategy> T getStrategy(Class<T> strategyKey) {
		IContentExchangeStrategy strategy = fStrategyIndex.get(strategyKey);
		if (strategy == null) {
			throw new NoSuchElementException(
					"Configuration error: unknown exchange strategy "
							+ strategyKey.getName() + ".");
		}
		
		// Will generate ClassCastException if client does a mistake.
		return (T) strategy; 
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		// Cloning here is tricky because of loose coupling. Therefore, calling
		// configure is the best way out, but it won't produce clones: only
		// fresh instances.
		try {
			SocialNewscastingService cloned = (SocialNewscastingService) super
					.clone();
			cloned.fStorage = (IWritableEventStorage) this.fStorage.clone();
			cloned.configure(configurator(fPrefix));
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Should never happen...
		}
	}

	// ----------------------------------------------------------------------
	// Logging methods.
	// ----------------------------------------------------------------------

	private void log(int len) {
		try {
			fTweetLog.write(fBuffer, 0, len);
		} catch(IOException ex) {
			throw new RuntimeException(ex);
		}
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

			log(fBuffer, TWEETED.magicNumber(), // event type
					tweet.poster.getID(), // tweeting node (us)
					tweet.sequenceNumber, // sequence number
					CommonState.getTime()); // simulation time

			// Looks up destinations and increase their pending count.
			int size = tweet.destinations();
			for (int i = 0; i < size; i++) {
				Node owner = tweet.destination(i);
				((SocialNewscastingService) owner.getProtocol(fProtocolID))
						.countPending();
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

			if (!duplicate) {
				log(fBuffer, DELIVER_SINGLE_TWEET.magicNumber(),// event type
						tweet.poster.getID(), // node owning the tweet
						sending.getID(), // the sending node.
						receiving.getID(), // node receiving the tweet
						tweet.sequenceNumber, // sequence number of the tweet
						CommonState.getTime()); // simulation time

				// One less pending tweet to receive.
				fPending--;
			}

			else {
				log(fBuffer, DUPLICATE_TWEET.magicNumber(),// event type
						tweet.poster.getID(), // node owning the tweet
						sending.getID(), // the sending node.
						receiving.getID(), // node receiving the tweet
						tweet.sequenceNumber, // sequence number of the tweet
						CommonState.getTime()); // simulation time
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
			log(fBuffer, EXCHANGE_DIGESTS.magicNumber(), // Event type
					sender.getID(), // ID of the digest sender.
					receiver.getID(), // ID of digest receiver.
					holes.size(), // Number of items in the digest.
					CommonState.getTime()); // Simulation time.

			fChannel.sendDigest(sender, receiver, owner, holes);
		}

		// ----------------------------------------------------------------------

		private void log(byte[] buf, Number... event) {
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

	public void resetCounters() {
		fPending = 0;
	}

	// ----------------------------------------------------------------------

}

// ----------------------------------------------------------------------

class StrategyEntry {

	public final IContentExchangeStrategy strategy;
	public final BinaryCompositeFilter filter;
	public final IReference<IPeerSelector> selector;

	public final double probability;

	public StrategyEntry(IContentExchangeStrategy strategy,
			IReference<IPeerSelector> selector, BinaryCompositeFilter filter,
			double probability) {
		this.strategy = strategy;
		this.selector = selector;
		this.filter = filter;
		this.probability = probability;
	}
}
