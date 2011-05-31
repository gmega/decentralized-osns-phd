package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.DenseMultiCounter;
import it.unitn.disi.utils.IKey;
import it.unitn.disi.utils.IMultiCounter;
import it.unitn.disi.utils.SparseMultiCounter;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import peersim.config.IResolver;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Classical rumor mongering algorithm by <a
 * href="http://doi.acm.org/10.1145/41840.41841"> Demers et al. </a>, adopted to
 * constrain information exchange to shared neighbors in a secondary graph. This
 * is the non-blind version.
 * 
 * @author giuliano
 */
public class DemersRumorMonger implements IContentExchangeStrategy,
		IEventObserver, ISelectionFilter {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------

	/**
	 * Probability with which a hot rumor will stop being transmitted.
	 */
	public static final String PAR_GIVEUP_PROBABILITY = "giveup_probability";

	/**
	 * How many rumors at a time this protocol will transmit.
	 */
	public static final String PAR_TRANSMIT_SIZE = "chunk_size";

	/**
	 * {@link Linkable} constraining dissemination for this protocol.
	 */
	public static final String PAR_LINKABLE = "linkable";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private int fRumorTransmitSize;

	private double fGiveup;

	private Random fRandom;

	private final int fProtocolId;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	private static final ArrayList<Boolean> fResponseBuffer = new ArrayList<Boolean>();

	private RumorList fRumorList;

	// ----------------------------------------------------------------------

	public DemersRumorMonger(IResolver resolver, String prefix, int protocolId,
			Node source, Random rnd) {
		this(resolver.getDouble(prefix, PAR_GIVEUP_PROBABILITY), resolver
				.getInt(prefix, PAR_TRANSMIT_SIZE), protocolId, source,
				(Linkable) source.getProtocol(resolver.getInt(prefix,
						PAR_LINKABLE)), rnd);
	}

	// ------------------------------------------------------------------------

	public DemersRumorMonger(double giveUp, int rumorTransmitSize,
			int protocolId, Node source, Linkable constraintLinkable, Random rnd) {
		fRumorTransmitSize = rumorTransmitSize;
		fProtocolId = protocolId;
		fGiveup = giveUp;
		fRandom = rnd;
		fRumorList = new RumorList(Integer.MAX_VALUE, constraintLinkable,
				fGiveup, fRandom);
	}

	// ----------------------------------------------------------------------

	/**
	 * Performs a non-blind rumor monger exchange between sender and receiver.
	 * This rumor monger object is assumed to be owned by the sender.
	 */
	public boolean doExchange(SNNode sender, SNNode receiver) {
		// Receiver is null. Returns.
		if (receiver == null) {
			return false;
		}

		IApplicationInterface application = (IApplicationInterface) receiver
				.getProtocol(fProtocolId);

		// Not exactly a great assumption to make.
		IProtocolSet set = (IProtocolSet) application;
		DemersRumorMonger rApp = (DemersRumorMonger) set
				.getStrategy(DemersRumorMonger.class);

		// Rumor mongering entails picking a certain number of the
		// "hottest" known rumors and passing them forward.
		int size = rApp.receiveRumor(receiver, sender, fRumorList.getList(),
				fResponseBuffer, fProtocolId, application);

		// Feedback is used to adjust the "hotness" of the rumors.
		fRumorList.demote(fResponseBuffer, size, sender);
		return true;
	}

	// ----------------------------------------------------------------------

	private int receiveRumor(SNNode ours, SNNode sender,
			List<IGossipMessage> outsideRumors,
			ArrayList<Boolean> responseBuffer, int protocolID,
			IApplicationInterface application) {

		ListIterator<IGossipMessage> it = outsideRumors.listIterator();
		int total = 0;
		int i = 0;
		/**
		 * Goes through the list of "receivable" rumors. We won't decide to
		 * receive a rumor until we know that it might be useful.
		 */
		for (i = 0; it.hasNext() && total < fRumorTransmitSize; i++) {
			IGossipMessage message = it.next();
			boolean wasNew = true;
			boolean deliver = false;
			if (!message.originator().equals(ours)) {
				deliver = true;
				// Is this rumor already known?
				if (!application.storage().contains(message)) {
					// Nope, tries to add to rumor list.
					deliver = fRumorList.add(message);
				}

				if (deliver) {
					wasNew = application.deliver(sender, ours, message, this);
					// Flags the original message as forwarded.
					message.forwarded(sender, ours);
					total++;
				}
			}

			// Note that if we don't deliver the message, wasNew will be true,
			// which means the rumor won't be demoted at the sender and it will
			// be as if he never sent it.
			responseBufferAppend(responseBuffer, i, wasNew);
		}
		return i;
	}

	// ----------------------------------------------------------------------

	private void responseBufferAppend(ArrayList<Boolean> responseBuffer, int i,
			boolean value) {
		if (i >= responseBuffer.size()) {
			responseBuffer.add(value);
		} else {
			responseBuffer.set(i, value);
		}
	}

	// ----------------------------------------------------------------------

	private void addTweet(IGossipMessage tweet) {
		fRumorList.add(tweet);
	}

	// ----------------------------------------------------------------------

	public int throttling(SNNode node) {
		return 1;
	}

	// ----------------------------------------------------------------------

	public ActivityStatus status() {
		return (fRumorList.size() == 0) ? ActivityStatus.QUIESCENT
				: ActivityStatus.ACTIVE;
	}

	// ----------------------------------------------------------------------
	// ISelectionFilter interface.
	// ----------------------------------------------------------------------

	@Override
	public boolean canSelect(Node node) {
		return fRumorList.messagesFor(node) != 0;
	}

	// ----------------------------------------------------------------------

	public Node selected(Node node) {
		// Don't care.
		return node;
	}

	// ----------------------------------------------------------------------
	// ICachingObject interface.
	// ----------------------------------------------------------------------

	@Override
	public void clear(Node source) {
		// No cache to clear.
	}

	// ----------------------------------------------------------------------
	// IEventObserver interface.
	// ----------------------------------------------------------------------

	@Override
	public void delivered(SNNode sender, SNNode receiver, IGossipMessage tweet,
			boolean duplicate) {
		if (!duplicate) {
			addTweet(tweet);
		}
	}

	// ----------------------------------------------------------------------

	@Override
	public void localDelivered(IGossipMessage message) {
		addTweet(message);
	}

	// ----------------------------------------------------------------------

}

// ----------------------------------------------------------------------

/**
 * Rumor list is an auxiliary object which helps the rumor mongering protocol
 * maintain and update its list of rumors.
 */
class RumorList implements Cloneable {

	private static IKey<Node> fKeyer = new IKey<Node>() {
		@Override
		public int key(Node element) {
			return (int) element.getID();
		}
	};

	/**
	 * The rumors we are currently transmitting.
	 */
	private LinkedList<IGossipMessage> fHotRumors = new LinkedList<IGossipMessage>();
	private List<IGossipMessage> fRoHotRumors = Collections
			.unmodifiableList(fHotRumors);

	/**
	 * Keeps track of destinations for messages.
	 */
	private IMultiCounter<Node> fDestinations;

	private Linkable fConstraint;

	/**
	 * See {@link DemersRumorMonger#PAR_GIVEUP_PROBABILITY}.
	 */
	private double fGiveupProbability;

	/**
	 * Maximum size for the hot rumor list. If the list overgrows this, the
	 * "coldest" rumors start being evicted.
	 */
	private int fMaxSize;

	/** Random number generator. */
	private Random fRandom;

	// ----------------------------------------------------------------------

	public RumorList(int maxSize, Linkable linkable, double giveupProbability,
			Random rnd) {
		fMaxSize = maxSize;
		fGiveupProbability = giveupProbability;
		fRandom = rnd;
		fConstraint = linkable;

		Node[] neighbors = new Node[linkable.degree()];
		for (int i = 0; i < neighbors.length; i++) {
			neighbors[i] = linkable.getNeighbor(i);
		}
		
		fDestinations = new SparseMultiCounter<Node>();
	}

	// ----------------------------------------------------------------------

	public boolean add(IGossipMessage evt) {
		if (addDestinations(evt) == 0) {
			return false;
		}

		// Hottest rumors are at the END of the list.
		fHotRumors.addLast(evt.cloneIfNeeded());
		if (fMaxSize > 0 && fHotRumors.size() > fMaxSize) {
			IGossipMessage discarded = fHotRumors.removeFirst();
			removeDestinations(discarded);
		}

		return true;
	}

	// ----------------------------------------------------------------------

	public int size() {
		return fHotRumors.size();
	}

	// ----------------------------------------------------------------------

	public List<IGossipMessage> getList() {
		return fRoHotRumors;
	}

	// ----------------------------------------------------------------------

	public void demote(ArrayList<Boolean> mask, int size, Node node) {
		ListIterator<IGossipMessage> it = fHotRumors.listIterator(start(size));

		for (int i = 0; it.hasNext() && i < size; i++) {
			// Rumor didn't help.
			if (!mask.get(i)) {
				// Either discards ...
				if (fRandom.nextDouble() < fGiveupProbability) {
					IGossipMessage discarded = it.next();
					it.remove();
					discarded.dropped(node);
					removeDestinations(discarded);
				}
				// .. or demotes the Tweet.
				else if (it.hasPrevious()) {
					/**
					 * This piece of code simply pushes an element one position
					 * back into the list. The painful dance with the iterators
					 * is to be efficient with the linked list implementation,
					 * and avoid O(n) access.
					 */
					IGossipMessage evt = it.next();
					it.previous();
					IGossipMessage previous = it.previous();
					it.set(evt);
					it.next();
					it.next();
					it.set(previous);
				} else {
					it.next();
				}
			} else {
				it.next();
			}
		}
	}

	// ----------------------------------------------------------------------

	private int start(int size) {
		return Math.max(0, fHotRumors.size() - size);
	}

	// ----------------------------------------------------------------------

	private int addDestinations(IGossipMessage tweet) {
		int size = tweet.destinations();
		int actual = 0;
		for (int i = 0; i < size; i++) {
			Node destination = tweet.destination(i);
			if (fConstraint.contains(destination)
					&& !destination.equals(tweet.originator())) {
				fDestinations.increment(destination);
				actual++;
			}
		}
		return actual;
	}

	// ----------------------------------------------------------------------

	private void removeDestinations(IGossipMessage tweet) {
		int size = tweet.destinations();
		for (int i = 0; i < size; i++) {
			Node destination = tweet.destination(i);
			if (fConstraint.contains(destination)
					&& !destination.equals(tweet.originator())) {
				fDestinations.decrement(destination);
			}
		}
	}

	// ----------------------------------------------------------------------

	public int messagesFor(Node neighbor) {
		return fDestinations.count(neighbor);
	}

	// ----------------------------------------------------------------------
	// Protocol interface.
	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			RumorList cloned = (RumorList) super.clone();
			cloned.fHotRumors = new LinkedList<IGossipMessage>(fHotRumors);
			cloned.fRoHotRumors = Collections
					.unmodifiableList(cloned.fHotRumors);

			return cloned;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}

// ----------------------------------------------------------------------
