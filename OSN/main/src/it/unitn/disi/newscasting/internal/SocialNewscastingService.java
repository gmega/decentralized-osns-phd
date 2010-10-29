package it.unitn.disi.newscasting.internal;

import it.unitn.disi.IInitializable;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.BinaryCompositeFilter;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.PeersimUtils;

import java.io.IOException;
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
		IApplicationInterface, IInitializable {

	private static final String CONFIGURATOR = "configurator";

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
	 * The node assigned to this instance of the social newscasting service.
	 */
	private Node fOwner;

	/**
	 * Our configuration prefix.
	 */
	private String fPrefix;

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

	private IApplicationConfigurator fConfigurator;

	private BroadcastBus fChannel;

	/**
	 * The configured exchange strategies.
	 */
	private HashMap<Class<? extends IContentExchangeStrategy>, StrategyEntry> fStrategyIndex;

	private ArrayList<StrategyEntry> fStrategies;

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	private IMessageVisibility fVisibility;

	// ----------------------------------------------------------------------

	public SocialNewscastingService(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("social_neighborhood") int socialNetworkId)
			throws IOException {

		this(prefix, PeersimUtils.selfPid(prefix), socialNetworkId,
				configurator(prefix));
	}

	// ----------------------------------------------------------------------

	public SocialNewscastingService(String prefix, int protocolID,
			int socialNetworkId, IApplicationConfigurator configurator) {
		fPrefix = prefix;
		fProtocolID = protocolID;
		fSocialNetworkID = socialNetworkId;
		fConfigurator = configurator;
	}

	// ----------------------------------------------------------------------

	private void configure() {
		this.flushState();
		try {
			fConfigurator.configure(this, fPrefix, fProtocolID,
					fSocialNetworkID);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------

	private void flushState() {
		fObserver = new MergeObserverImpl();
		fStrategyIndex = new HashMap<Class<? extends IContentExchangeStrategy>, StrategyEntry>();
		fStrategies = new ArrayList<StrategyEntry>();
		fChannel = new BroadcastBus();
		fVisibility = new DefaultVisibility(fSocialNetworkID);
	}

	// ----------------------------------------------------------------------

	private static IApplicationConfigurator configurator(String prefix) {
		if (!Configuration.contains(prefix + "." + CONFIGURATOR)) {
			return new NewscastAppConfigurator(prefix);
		} else {
			return (IApplicationConfigurator) Configuration.getInstance(prefix
					+ "." + CONFIGURATOR);
		}
	}

	// ----------------------------------------------------------------------
	// Configuration callbacks.
	// ----------------------------------------------------------------------

	public void addStrategy(Class<? extends IContentExchangeStrategy>[] keys,
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
			fStrategyIndex.put(key, entry);
		}
	}

	// ----------------------------------------------------------------------

	public void setStorage(IWritableEventStorage storage) {
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
			StrategyEntry entry = fStrategies.get(i);

			if (!runStrategy(entry)) {
				continue;
			}

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

	private boolean runStrategy(StrategyEntry entry) {
		boolean shouldRun = true;
		// A strategy is run if:
		// 1 - it's not quiescent;
		shouldRun &= entry.strategy.status() != IContentExchangeStrategy.ActivityStatus.QUIESCENT;
		// 2 - it passes the dice test.
		shouldRun &= (CommonState.r.nextDouble() < entry.probability);
		return shouldRun;
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

	public Tweet postToFriends() {
		return this.replyToPost(null);
	}

	// ----------------------------------------------------------------------

	public Tweet replyToPost(Tweet replyTo) {
		// Sanity check.
		if (replyTo != null && !storage().contains(replyTo)) {
			throw new IllegalArgumentException(
					"Can't reply to an unknown post.");
		}

		// As per the contract.
		Tweet tweet = new Tweet(fOwner, ++fSeqNumber, fVisibility, replyTo);
		fStorage.add(tweet);

		// Notifies everyone.
		fObserver.tweeted(tweet);

		return tweet;
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

	public void removeSubscriber(IEventObserver observer) {
		fChannel.removeSubscriber(observer);
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
	public void clear(Node node) {
		for (StrategyEntry entry : fStrategies) {
			entry.strategy.clear(node);
			entry.selector.get(node).clear(node);
		}
	}

	// ----------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T extends IContentExchangeStrategy> T getStrategy(
			Class<T> strategyKey) {
		// Will generate ClassCastException if client does a mistake.
		return (T) lookupEntry(strategyKey).strategy;
	}

	// ----------------------------------------------------------------------

	@Override
	public IReference<ISelectionFilter> getFilter(
			Class<? extends IContentExchangeStrategy> strategyKey) {
		return lookupEntry(strategyKey).filter.right();
	}

	// ----------------------------------------------------------------------

	@Override
	public IReference<IPeerSelector> getSelector(
			Class<? extends IContentExchangeStrategy> strategyKey) {
		return lookupEntry(strategyKey).selector;
	}

	// ----------------------------------------------------------------------

	public int pid() {
		return fProtocolID;
	}

	// ----------------------------------------------------------------------

	private StrategyEntry lookupEntry(Class<?> strategyKey) {
		StrategyEntry entry = fStrategyIndex.get(strategyKey);
		if (entry == null) {
			throw new NoSuchElementException(
					"Configuration error: unknown exchange strategy "
							+ strategyKey.getName() + ".");
		}
		return entry;
	}
	
	// ----------------------------------------------------------------------
	// IInitializable interface.
	// ----------------------------------------------------------------------

	@Override
	public void initialize(Node node) {
		fOwner = node;
		this.configure();
	}
	
	@Override
	public void reinitialize() {
		// XXX Tell protocol parts that node rejoined the network.
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		// Cloning here is tricky because of loose coupling. 
		try {
			SocialNewscastingService cloned = (SocialNewscastingService) super
					.clone();
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Should never happen...
		}
	}

	// ----------------------------------------------------------------------
	// IMergeObserver interface.
	// ----------------------------------------------------------------------

	private class MergeObserverImpl implements IMergeObserver {

		// ----------------------------------------------------------------------

		public void tweeted(Tweet tweet) {
			// Sanity check.
			if (fOwner != tweet.poster) {
				throw new IllegalStateException(
						"Node can only tweet in its turn.");
			}

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
				// One less pending tweet to receive.
				fPending--;
			}

			// Passes the event forward.
			fChannel.eventDelivered(sending, receiving, tweet, duplicate);
		}

		// ----------------------------------------------------------------------

		public void sendDigest(Node sender, Node receiver, Node owner,
				List<Integer> holes) {
			fChannel.sendDigest(sender, receiver, owner, holes);
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
