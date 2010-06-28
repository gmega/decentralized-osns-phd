package it.unitn.disi.application;

import static it.unitn.disi.application.NewscastEvents.DELIVER_SINGLE_TWEET;
import static it.unitn.disi.application.NewscastEvents.DELIVER_TWEET_RANGE;
import static it.unitn.disi.application.NewscastEvents.DUPLICATE_TWEET;
import static it.unitn.disi.application.NewscastEvents.EXCHANGE_DIGESTS;
import static it.unitn.disi.application.NewscastEvents.TWEETED;
import it.unitn.disi.application.EventRegistry.Info;
import it.unitn.disi.application.interfaces.IContentExchangeStrategy;
import it.unitn.disi.application.interfaces.IEventObserver;
import it.unitn.disi.application.interfaces.IPeerSelector;
import it.unitn.disi.application.interfaces.ISelectionFilter;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.LogManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * The newscast application simulates a simplified twitter-like message exchange
 * system.
 * 
 * @author giuliano
 */
public class NewscastApplication implements CDProtocol, IApplication {

	// ----------------------------------------------------------------------
	// Configuration parameters keys.
	// ----------------------------------------------------------------------
	
	/**
	 * {@link Linkable} containing the social neighborhood of the node.
	 */
	private static final String PAR_SOCIAL_NEIGHBORHOOD = "social_neighborhood";
	
	/**
	 * A node receiving more than {@link #PAR_CONN_LIMIT} connections per round
	 * will reject them.
	 */
	private static final String PAR_CONN_LIMIT = "connection_limit";

	/**
	 * Parameter indicating the per-round probability for a node to decide to
	 * tweet.
	 */
	private static final String PAR_TWEET_PROBABILITY = "tweet_probability";

	/**
	 * Paramater which tunes at which round the application should stop
	 * tweeting.
	 */
	private static final String PAR_TWEET_UNTIL = "tweet_until";
	
	/**
	 * Causes logging messages to be printed as text as well.
	 */
	private static final String PAR_VERBOSE = "verbose";
	
	/**
	 * Enables a variety of internal consistency checks, at the expense of
	 * runtime performance.
	 */
	private static final String PAR_DEBUG = "debug";

	// ----------------------------------------------------------------------
	// Other constants.
	// ----------------------------------------------------------------------

	/** Constant representing plus infinite. */
	private static final int FOREVER = Integer.MAX_VALUE;

	// ----------------------------------------------------------------------
	// Logging constants and shared structures.
	// ----------------------------------------------------------------------

	/**
	 * String identifier for the log in which tweets are written to.
	 */
	private static final String TWEET_LOG = "tweets";

	private static final EventCodec fCodec = new EventCodec(Byte.class,
			NewscastEvents.values());

	protected static final byte[] fBuffer = new byte[NewscastEvents.set
			.sizeof(NewscastEvents.set.getLargest())];

	// ----------------------------------------------------------------------
	// Storage for peersim parameters.
	// ----------------------------------------------------------------------
	
	private int fProtocolID;
	
	private int fSocialNetworkID;

	private int fTweetUntil;

	private int fMaxRoundCalls;
	
	private double fTweetProbability;
	
	private boolean fDebug;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	/** Storage for events. */
	private EventStorage fStorage = new EventStorage();

	/** Tweet sequence number. */
	private int fSeqNumber = 0;

	/** Calls received in the current round. */
	private int fRoundCalls;

	/** Calls received in total. */
	private int fContacts = 0;
	
	/** Messages pending delivery to this app. */
	private int fPending;

	// ----------------------------------------------------------------------
	// Internal instances (so that the class doesn't expose these interfaces).
	// ----------------------------------------------------------------------

	private SelectionFilter fFilter;

	private MergeObserverImpl fObserver;

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	/**
	 * Channel for events at the {@link NewscastApplication}. Careful with how
	 * you wire it, or infinite loops may ensue.
	 */
	private BroadcastBus fChannel;
	
	private ArrayList<StrategyEntry> fStrategies;

	private Map<AdapterKey, Object> fAdapters;
	
	private boolean fSuppressTweeting;
	
	private boolean fVerbose;

	private LogManager fManager;

	private String fPrefix;
	
	// ----------------------------------------------------------------------

	public NewscastApplication(String prefix) throws IOException {
		// Inits the log manager.
		fPrefix = prefix;
		fManager = LogManager.getInstance();
		fManager.add(prefix);

		// Local parameters.
		fSocialNetworkID = Configuration.getPid(prefix + "." + PAR_SOCIAL_NEIGHBORHOOD);
		fMaxRoundCalls = Configuration.getInt(prefix + "." + PAR_CONN_LIMIT,
				Integer.MAX_VALUE);
		fTweetProbability = Configuration.getDouble(prefix + "."
				+ PAR_TWEET_PROBABILITY);
		fTweetUntil = Configuration.getInt(prefix + "." + PAR_TWEET_UNTIL,
				FOREVER);
		fVerbose = Configuration.contains(prefix + "." + PAR_VERBOSE);
		fDebug = Configuration.contains(prefix + "." + PAR_DEBUG);
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
		fFilter = new SelectionFilter();
		fObserver = new MergeObserverImpl();
		fAdapters = new HashMap<AdapterKey, Object>();
		fStrategies = new ArrayList<StrategyEntry>();
		fChannel = new BroadcastBus();
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
		fStrategies.add(new StrategyEntry(strategy, selector, filter,
				probability));
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
		fRoundCalls = 0;

		fFilter.setProtocolID(protocolID);

		if (fDebug) {
			fStorage.consistencyCheck();
		}

		/**
		 * With probability P, "tweets".
		 */
		if (shouldTweet()) {
			tweet(ourNode, protocolID);
		}

		/**
		 * Runs the configured exchange strategies, with the configured peer
		 * selectors.
		 */
		for (int i = 0; i < fStrategies.size(); i++) {
			if (!runStrategy(i)) {
				continue;
			}
			
			StrategyEntry entry = fStrategies.get(i);
			IContentExchangeStrategy strategy = entry.strategy;
			IPeerSelector selector = entry.selector.get(ourNode);
			ISelectionFilter filter = entry.filter.get(ourNode);

			// Selects a peer using the preconfigured filter.
			Node peer;
			if (selector.supportsFiltering()) {
				peer = selector.selectPeer(ourNode, filter);
			} else {
				peer = selector.selectPeer(ourNode);
			}

			// No peer from this selector -- this strategy skips the round.
			if (peer == null) {
				continue;
			}
			
			// Increases the selection count.
			NewscastApplication other = (NewscastApplication) peer.getProtocol(protocolID);
			other.contacted();

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

	}

	// ----------------------------------------------------------------------

	private boolean runStrategy(int i) {
		if (CommonState.r.nextDouble() < fStrategies.get(i).probability) {
			return true;
		}

		return false;
	}

	// ----------------------------------------------------------------------

	private void contacted() {
		fContacts++;
		fRoundCalls++;
	}
	
	// ----------------------------------------------------------------------

	private void countPending() {
		fPending++;
	}

	// ----------------------------------------------------------------------

	private void tweet(Node us, int protocolID) {
		Linkable socialNetwork = socialNetwork(us);
		
		Tweet evt = new Tweet(us, ++fSeqNumber);
		fStorage.add(us, evt.fSequence);

		// Notifies everyone.
		fObserver.tweeted(us, fSeqNumber);

		if (fDebug) {
			EventRegistry.getInstance().addEvent(evt,
					new Info(CommonState.getTime(), socialNetwork.degree()));
		}
	}

	// ----------------------------------------------------------------------

	private boolean shouldTweet() {
		if (fSuppressTweeting || ((int) CommonState.getTime()) > fTweetUntil) {
			return false;
		}
		return CommonState.r.nextDouble() < fTweetProbability;
	}

	// ----------------------------------------------------------------------
	// IApplication interface. 
	// ----------------------------------------------------------------------
	
	public boolean receiveTweet(Node sender, Node ours,
			Tweet tweet, IEventObserver broadcaster) {
		
		// This is kind of an ugly hack...
		fChannel.beginBroadcast(broadcaster);		
		
		if (fStorage.contains(tweet.fNode, tweet.fSequence)) {
			fObserver.duplicateReceived(sender, ours, tweet.fNode, -1,
					tweet.fSequence);
			return false;
		}

		fStorage.add(tweet.fNode, tweet.fSequence);
		fObserver
				.eventDelivered(sender, ours, tweet.fNode, -1, tweet.fSequence);
		return true;
	}
	
	// ----------------------------------------------------------------------
	
	public boolean knows(Tweet tweet) {
		return knows(tweet.fNode, tweet.fSequence);
	}
	
	// ----------------------------------------------------------------------
	
	public boolean knows(Node node, int sequence) {
		return fStorage.contains(node, sequence);
	}
	
	// ----------------------------------------------------------------------
	
	public boolean toggleTweeting() {
		fSuppressTweeting = !fSuppressTweeting;
		return !fSuppressTweeting;
	}
	
	// ----------------------------------------------------------------------
	
	public boolean isSuppressingTweets() {
		return fSuppressTweeting;
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
			NewscastApplication cloned = (NewscastApplication) super.clone();
			cloned.fStorage = (EventStorage) this.fStorage.clone();
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

		public void tweeted(Node owner, int sequenceNumber) {
			log(fBuffer, TWEETED.magicNumber(), // event
					// type
					owner.getID(), // tweeting node (us)
					sequenceNumber, // sequence number
					CommonState.getTime()); // simulation time
			
			// Declares to our neighbors that they have an extra pending message.
			Linkable sn = socialNetwork(owner);
			int degree = sn.degree();
			for (int i = 0; i < degree; i++) {
				((NewscastApplication) owner.getProtocol(fProtocolID)).countPending();
			}
			
			fChannel.tweeted(owner, fSeqNumber);
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

		public void duplicateReceived(Node sender, Node receiver, Node owner,
				int start, int end) {
			log(fBuffer, DUPLICATE_TWEET
					.magicNumber(), // event type
					sender.getID(), // id of the sender
					receiver.getID(), // id of the receiver
					owner.getID(), // id of the tweeting node
					end, // event sequence number
					CommonState.getTime()); // simulation time
			

			fChannel
					.duplicateReceived(sender, receiver, owner, start, end);
		}

		/**
		 * Registers the reception of a range of tweet events.
		 */
		public void eventDelivered(Node sending, Node receiving, Node tweeting,
				int start, int finish) {
			// Sanity check.
			if (tweeting.equals(receiving)) {
				throw new InternalError();
			}

			if (start == -1) {
				log(fBuffer, DELIVER_SINGLE_TWEET
						.magicNumber(), // event type
						tweeting.getID(), // node owning the tweet
						sending.getID(), // the sending node.
						receiving.getID(), // node receiving the tweet
						finish, // sequence number of the tweet
						CommonState.getTime()); // simulation time
			} else {
				log(fBuffer, DELIVER_TWEET_RANGE
						.magicNumber(), // event type
						tweeting.getID(), // node owning the tweet
						sending.getID(), receiving.getID(), // node receiving
						// the
						// tweet
						start, // start of event range being received
						finish, // end of event range being received
						CommonState.getTime()); // simulation time
			}

			// Accounts for the pending messages we just received.
			int real_start = (start == -1) ? finish : start;
			fPending -= (finish - real_start + 1);
			assert fPending > 0;
			
			if (fDebug) {
				for (int i = real_start; i <= finish; i++) {
					EventRegistry.getInstance()
							.received(new Tweet(tweeting, i));
				}
			}

			fChannel
					.eventDelivered(sending, receiving, tweeting, start, finish);
		}
		
		private void log(byte[] buf, Number...event) {
			if (fVerbose) {
				System.out.println(fCodec.toString(event));
			}

			int len = fCodec.encodeEvent(buf, 0, event);
			NewscastApplication.this.log(len);
		}
	};

	// ----------------------------------------------------------------------
	// ISelectionFilter interface.
	// ----------------------------------------------------------------------
	private class SelectionFilter implements ISelectionFilter {

		private int fProtocolId = -1;

		public void setProtocolID(int protocolID) {
			fProtocolId = protocolID;
		}

		public Node selected(Node node) {
			NewscastApplication app = (NewscastApplication) node
					.getProtocol(fProtocolId);
			app.contacted();
			return node;
		}

		public boolean canSelect(Node node) {
			NewscastApplication app = (NewscastApplication) node
					.getProtocol(fProtocolId);
			return app.fRoundCalls < fMaxRoundCalls;
		}

		public Object clone() {
			throw new UnsupportedOperationException();
		}
	};
	
	// ----------------------------------------------------------------------
	// Debugging methods
	// ----------------------------------------------------------------------

	EventStorage getStorage() {
		return fStorage;
	}

	// ----------------------------------------------------------------------

	public int contacts() {
		return fContacts;
	}
	
	// ----------------------------------------------------------------------

	public void resetCounters() {
		fPending = fContacts = 0;
	}

	// ----------------------------------------------------------------------

	public boolean onDebug() {
		return fDebug;
	}
	
	// ----------------------------------------------------------------------

	public int realtimeDrift(Node node, int protocolID) {
		return this.realtimeDrift(node, protocolID, false);
	}

	// ----------------------------------------------------------------------

	public int realtimeDrift(Node node, int protocolID, boolean verbose) {
		Linkable socialNet = socialNetwork(node);
		int drift = 0;

		for (int i = 0; i < socialNet.degree(); i++) {
			Node neighbor = socialNet.getNeighbor(i);

			// Neighbor does not exist yet, so we cannot possibly
			// have drifted with respect to it.
			if (neighbor == null) {
				continue;
			}

			NewscastApplication neighborApp = (NewscastApplication) neighbor
					.getProtocol(protocolID);

			// Counts the holes in the list.
			List<Integer> ourList = getStorage().getList(neighbor);
			Integer lastKnown = 0;
			if (verbose) {
				System.err.print(neighbor.getID() + ": [ ");
			}

			if (ourList != null) {
				ListIterator<Integer> it = ourList.listIterator();
				while (it.hasNext()) {
					Integer start = it.next();
					drift += (start - (lastKnown + 1));
					lastKnown = it.next();
					if (verbose) {
						System.err.print("(" + start + "," + lastKnown + ") ");
					}
				}
			}

			if (verbose) {
				System.err.println(" ] -> " + neighborApp.fSeqNumber);
			}

			if (lastKnown > neighborApp.fSeqNumber) {
				throw new AssertionError();
			}

			drift += (neighborApp.fSeqNumber - lastKnown);
		}

		return drift;
	}

	// ----------------------------------------------------------------------

	public static final int LATENCY = 0;

	public static final int NEIGHBOR = 1;

	public void maxDelay(Node node, int protocolID, long[] response) {

		if (response.length < 2) {
			throw new IllegalArgumentException();
		}

		response[LATENCY] = response[NEIGHBOR] = -1;

		Linkable socialNet = socialNetwork(node);
		int maxDelay = 0;
		for (int i = 0; i < socialNet.degree(); i++) {
			Node neighbor = socialNet.getNeighbor(i);
			NewscastApplication neighborApp = (NewscastApplication) neighbor
					.getProtocol(protocolID);

			// Computes the maximum delay.
			List<Integer> ourList = getStorage().getList(neighbor);
			Integer lastKnown = 0;

			if (ourList != null) {
				if (ourList.size() != 2) {
					throw new AssertionError();
				}
				lastKnown = ourList.get(ourList.size() - 1);
			}

			if (lastKnown > neighborApp.fSeqNumber) {
				throw new AssertionError();
			}

			for (int j = lastKnown + 1; j <= neighborApp.fSeqNumber; j++) {
				Tweet evt = new Tweet(neighbor, j);
				int delay = (int) CommonState.getTime()
						- EventRegistry.getInstance().getTime(evt);
				if (delay > maxDelay) {
					maxDelay = delay;
					response[NEIGHBOR] = neighbor.getID();
				}
			}
		}

		response[LATENCY] = maxDelay;
	}

}

// ----------------------------------------------------------------------

/**
 * {@link IApplicationConfigurator} knows how to configure the
 * {@link NewscastApplication}.
 */
interface IApplicationConfigurator {
	/**
	 * Configures the application. Configurator might assume that the instance
	 * is clean; i.e., has no configured strategies whatsoever.
	 */
	void configure(NewscastApplication app, int protocolId, int socialNetworkId);
}

//----------------------------------------------------------------------

class BroadcastBus implements IMergeObserver {
	
	private final ArrayList<IEventObserver> fDelegates = new ArrayList<IEventObserver>();
	
	private IEventObserver fCurrentBroadcaster;
	
	public void beginBroadcast(IEventObserver observer) {
		fCurrentBroadcaster = observer;
	}
	
	public void addSubscriber(IEventObserver observer) {
		fDelegates.add(observer);
	}

	public void sendDigest(Node sender, Node receiver, Node owner,
			List<Integer> holes) {
		for (IEventObserver observer : fDelegates) {
			if (observer instanceof IMergeObserver && fCurrentBroadcaster != observer) {
				((IMergeObserver)observer).sendDigest(sender, receiver, owner, holes);
			}
		}
		fCurrentBroadcaster = null;
	}
	
	public void tweeted(Node owner, int seqNumber) {
		for (IEventObserver observer : fDelegates) {
			if (fCurrentBroadcaster != observer) {
				observer.tweeted(owner, seqNumber);
			}
		}
		fCurrentBroadcaster = null;
	}

	public void duplicateReceived(Node sender, Node receiver, Node owner,
			int start, int end) {
		for (IEventObserver observer : fDelegates) {
			if (fCurrentBroadcaster != observer) {
				observer.duplicateReceived(sender, receiver, owner, start, end);
			}
		}
		fCurrentBroadcaster = null;
	}

	public void eventDelivered(Node sender, Node receiver, Node owner,
			int start, int end) {
		for (IEventObserver observer : fDelegates) {
			if (fCurrentBroadcaster != observer) {
				observer.eventDelivered(sender, receiver, owner, start, end);
			}
		}
		fCurrentBroadcaster = null;
	}
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
	public final IReference<ISelectionFilter> filter;
	public final IReference<IPeerSelector> selector;

	public final double probability;

	public StrategyEntry(IContentExchangeStrategy strategy,
			IReference<IPeerSelector> selector,
			IReference<ISelectionFilter> filter, double probability) {
		this.strategy = strategy;
		this.selector = selector;
		this.filter = filter;
		this.probability = probability;
	}
}
