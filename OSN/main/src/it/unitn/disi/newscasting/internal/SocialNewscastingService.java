package it.unitn.disi.newscasting.internal;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.BinaryCompositeFilter;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.IInitializable;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.SNNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
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

	private static final int DEFAULT_STRATEGIES = 1;

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
	private SNNode fOwner;

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

	private StrategyEntry[] fStrategies;

	private IResolver fResolver;

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	private IMessageVisibility fVisibility;

	// ----------------------------------------------------------------------

	public SocialNewscastingService(
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("social_neighborhood") int socialNetworkId)
			throws IOException {

		this(prefix, PeersimUtils.selfPid(prefix), socialNetworkId,
				configurator(prefix, resolver));
		fResolver = resolver;
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
			fConfigurator.configure(this, fResolver, fPrefix, fProtocolID,
					fSocialNetworkID);
			compact();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------

	private void flushState() {
		fObserver = new MergeObserverImpl();
		fStrategyIndex = new HashMap<Class<? extends IContentExchangeStrategy>, StrategyEntry>();
		fStrategies = new StrategyEntry[DEFAULT_STRATEGIES];
		fChannel = new BroadcastBus();
		fVisibility = new DefaultVisibility(fSocialNetworkID);
	}

	// ----------------------------------------------------------------------

	private static IApplicationConfigurator configurator(String prefix,
			IResolver resolver) {
		@SuppressWarnings("unchecked")
		Class<? extends IApplicationConfigurator> klass = Configuration
				.getClass(prefix + "." + CONFIGURATOR,
						NewscastAppConfigurator.class);
		ObjectCreator creator = new ObjectCreator(resolver);
		try {
			return creator.create(prefix, klass);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------
	// Configuration callbacks.
	// ----------------------------------------------------------------------

	public void addStrategy(Class<? extends IContentExchangeStrategy>[] keys,
			IContentExchangeStrategy strategy,
			IReference<IPeerSelector> selector,
			IReference<ISelectionFilter> filter) {

		StrategyEntry entry = new StrategyEntry(strategy, selector,
				new BinaryCompositeFilter(
						new FallThroughReference<ISelectionFilter>(
								ISelectionFilter.UP_FILTER), filter));

		strategyArrayAdd(entry);

		for (Class<? extends IContentExchangeStrategy> key : keys) {
			fStrategyIndex.put(key, entry);
		}
	}

	// ----------------------------------------------------------------------

	private void strategyArrayAdd(StrategyEntry entry) {
		if (fStrategies[fStrategies.length - 1] != null) {
			fStrategies = Arrays.copyOf(fStrategies, fStrategies.length * 2);
		}

		int idx = MiscUtils.lastDifferentFrom(fStrategies, null);
		fStrategies[idx + 1] = entry;
	}

	// ----------------------------------------------------------------------

	private void compact() {
		fStrategies = Arrays.copyOf(fStrategies,
				MiscUtils.lastDifferentFrom(fStrategies, null) + 1);
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
		for (int i = 0; i < fStrategies.length; i++) {
			StrategyEntry entry = fStrategies[i];

			if (!runStrategy(entry)) {
				continue;
			}

			SNNode peer = selectPeer(ourNode, entry.selector.get(ourNode),
					entry.filter);

			performExchange((SNNode) ourNode, peer, entry.strategy);
		}
	}

	// ----------------------------------------------------------------------

	private SNNode selectPeer(Node ourNode, IPeerSelector selector,
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
		return (SNNode) peer;
	}

	// ----------------------------------------------------------------------

	private void performExchange(SNNode ourNode, SNNode peer,
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
		return entry.strategy.status() != IContentExchangeStrategy.ActivityStatus.QUIESCENT;
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
	public boolean receiveTweet(SNNode sender, SNNode ours, Tweet tweet,
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
		fOwner = (SNNode) node;
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
				if (owner != tweet.poster) {
					((SocialNewscastingService) owner.getProtocol(fProtocolID))
							.countPending();
				}
			}

			fChannel.tweeted(tweet);
		}

		// ----------------------------------------------------------------------

		/**
		 * Registers the reception of a range of tweet events.
		 */
		public void eventDelivered(SNNode sending, SNNode receiving,
				Tweet tweet, boolean duplicate) {

			// Sanity check -- no protocol should do this.
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

	public StrategyEntry(IContentExchangeStrategy strategy,
			IReference<IPeerSelector> selector, BinaryCompositeFilter filter) {
		this.strategy = strategy;
		this.selector = selector;
		this.filter = filter;
	}
}
