package it.unitn.disi.epidemics;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.BinaryCompositeFilter;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.BroadcastBus;
import it.unitn.disi.newscasting.internal.IApplicationConfigurator;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.newscasting.internal.NewscastAppConfigurator;
import it.unitn.disi.newscasting.internal.SocialNewscastingService;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.IInitializable;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.NoSuchElementException;

import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.core.Node;

/**
 * {@link ProtocolRunner} provides a pre-cooked infrastructure for building,
 * configuring, and integrating gossip protocols.
 * 
 * @author giuliano
 * 
 * @param <M>
 * @param <P>
 */
@AutoConfig
public class ProtocolRunner implements IProtocolSet, IApplicationInterface,
		IInitializable, CDProtocol {

	// ----------------------------------------------------------------------
	//
	// ----------------------------------------------------------------------

	private static final String CONFIGURATOR = "configurator";

	private static final int DEFAULT_STRATEGIES = 1;

	// ----------------------------------------------------------------------
	// Configuration parameters.
	// ----------------------------------------------------------------------

	/**
	 * Our own protocol ID.
	 */
	private final int fProtocolID;

	/**
	 * The node assigned to this {@link ProtocolRunner}.
	 */
	private SNNode fOwner;

	/**
	 * Our configuration prefix.
	 */
	private String fPrefix;

	// ----------------------------------------------------------------------
	// Configuration stuff.
	// ----------------------------------------------------------------------

	/**
	 * Configuration script.
	 */
	private IApplicationConfigurator fConfigurator;

	/**
	 * Reference to PeerSim resolver.
	 */
	private IResolver fResolver;

	/**
	 * The configured exchange strategies.
	 */
	private HashMap<Class<? extends IContentExchangeStrategy>, StrategyEntry> fStrategyIndex;

	/**
	 * Alternate collection for strategies for efficient iteration.
	 */
	private StrategyEntry[] fStrategies;

	// ----------------------------------------------------------------------
	// Message and eventing management.
	// ----------------------------------------------------------------------

	protected MergeObserverImpl fObserver;

	private BroadcastBus fChannel;

	/**
	 * Storage for events.
	 */
	private IWritableEventStorage fStorage;

	/** Messages pending delivery to this app. */
	private int fPending;

	// ----------------------------------------------------------------------

	public ProtocolRunner(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix) {
		this(prefix, PeersimUtils.selfPid(prefix), configurator(prefix,
				resolver));
		fResolver = resolver;
	}

	// ----------------------------------------------------------------------

	public ProtocolRunner(String prefix, int protocolID,
			IApplicationConfigurator configurator) {
		fPrefix = prefix;
		fProtocolID = protocolID;
		fConfigurator = configurator;
	}

	// ----------------------------------------------------------------------

	private void configure() {
		this.flushState();
		try {
			fConfigurator.configure(this, fResolver, fPrefix);
			compact();
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------

	public void flushState() {
		fObserver = new MergeObserverImpl();
		fStrategyIndex = new HashMap<Class<? extends IContentExchangeStrategy>, StrategyEntry>();
		fStrategies = new StrategyEntry[DEFAULT_STRATEGIES];
		fChannel = new BroadcastBus();
	}

	// ----------------------------------------------------------------------

	private static IApplicationConfigurator configurator(String prefix,
			IResolver resolver) {
		@SuppressWarnings("unchecked")
		Class<? extends IApplicationConfigurator> klass = Configuration
				.getClass(prefix + "." + CONFIGURATOR);
		ObjectCreator creator = new ObjectCreator(resolver);
		try {
			return creator.create(prefix, klass);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	// ----------------------------------------------------------------------
	// ICoreInterface
	// ----------------------------------------------------------------------

	@Override
	public boolean deliver(SNNode sender, SNNode ours, IGossipMessage message,
			IEventObserver broadcaster) {
		// This is kind of an ugly hack...
		fChannel.beginBroadcast(broadcaster);

		boolean added = fStorage.add(message);
		fObserver.delivered(sender, ours, message, !added);

		return added;
	}

	// ----------------------------------------------------------------------

	@Override
	public IEventStorage storage() {
		return fStorage;
	}

	// ----------------------------------------------------------------------

	@Override
	public void addSubscriber(IEventObserver observer) {
		fChannel.addSubscriber(observer);
	}

	// ----------------------------------------------------------------------

	@Override
	public void removeSubscriber(IEventObserver observer) {
		fChannel.removeSubscriber(observer);
	}

	// ----------------------------------------------------------------------

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

	@Override
	public Node node() {
		return fOwner;
	}

	// ----------------------------------------------------------------------
	// IInitializable interface.
	// ----------------------------------------------------------------------

	@Override
	public void initialize(Node node) {
		fOwner = (SNNode) node;
		this.configure();
		for (StrategyEntry entry : fStrategies) {
			tryInitialize(entry.filter, node);
			tryInitialize(entry.selector, node);
			tryInitialize(entry.strategy, node);
		}
	}

	// ----------------------------------------------------------------------

	private void tryInitialize(Object object, Node node) {
		if (object instanceof IInitializable) {
			IInitializable initializable = (IInitializable) object;
			initializable.initialize(node);
		}
	}

	// ----------------------------------------------------------------------

	@Override
	public void reinitialize() {
		// XXX Tell protocol parts that node rejoined the network.
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

	private void addPending() {
		fPending++;
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

	public void compact() {
		fStrategies = Arrays.copyOf(fStrategies,
				MiscUtils.lastDifferentFrom(fStrategies, null) + 1);
	}

	// ----------------------------------------------------------------------

	public void setStorage(IWritableEventStorage storage) {
		fStorage = storage;
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		// Cloning here is tricky because of loose coupling.
		try {
			ProtocolRunner cloned = (ProtocolRunner) super.clone();
			return cloned;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // Should never happen...
		}
	}

	// ----------------------------------------------------------------------
	// IMergeObserver interface.
	// ----------------------------------------------------------------------

	protected class MergeObserverImpl implements IEventObserver {

		// ----------------------------------------------------------------------

		@Override
		@SuppressWarnings("unchecked")
		public void localDelivered(IGossipMessage message) {

			fStorage.add(message);
			// Looks up destinations and increase their pending count.
			int size = message.destinations();
			for (int i = 0; i < size; i++) {
				Node destination = message.destination(i);
				if (destination != message.originator()) {
					((ProtocolRunner) destination.getProtocol(fProtocolID))
							.addPending();
				}
			}
			fChannel.localDelivered(message);
		}

		// ----------------------------------------------------------------------

		/**
		 * Registers the reception of a range of tweet events.
		 */
		@Override
		public void delivered(SNNode sending, SNNode receiving,
				IGossipMessage message, boolean duplicate) {

			// Sanity check -- no protocol should do this.
			if (message.originator().equals(receiving)) {
				throw new InternalError();
			}

			if (!duplicate) {
				// One less pending tweet to receive.
				fPending--;
			}

			// Passes the event forward.
			fChannel.delivered(sending, receiving, message, duplicate);
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
