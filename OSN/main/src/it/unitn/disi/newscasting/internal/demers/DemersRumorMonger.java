package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MultiCounter;
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

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private IReference<Linkable> fConstraintLinkable;

	private int fRumorTransmitSize;

	private final int fProtocolId;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	private static final ArrayList<Boolean> fResponseBuffer = new ArrayList<Boolean>();

	private RumorList fRumorList;

	// ----------------------------------------------------------------------

	public DemersRumorMonger(IResolver resolver, String prefix, int protocolId,
			IReference<Linkable> constraintLinkable, Random rnd) {
		this(resolver.getDouble(prefix, PAR_GIVEUP_PROBABILITY), resolver
				.getInt(prefix, PAR_TRANSMIT_SIZE), protocolId,
				constraintLinkable, rnd);
	}

	// ----------------------------------------------------------------------

	public DemersRumorMonger(double giveUp, int rumorTransmitSize,
			int protocolId, IReference<Linkable> constraintLinkable, Random rnd) {
		fRumorList = new RumorList(Integer.MAX_VALUE, giveUp, rnd);
		fRumorTransmitSize = rumorTransmitSize;
		fProtocolId = protocolId;
		fConstraintLinkable = constraintLinkable;
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

		ICoreInterface application = (ICoreInterface) receiver
				.getProtocol(fProtocolId);
		DemersRumorMonger rApp = (DemersRumorMonger) application
				.getStrategy(DemersRumorMonger.class);

		// Rumor mongering entails picking a certain number of the
		// "hottest" known rumors and passing them forward.
		int size = rApp.receiveRumor(receiver, sender, fRumorList.getList(),
				fResponseBuffer, fProtocolId, application);

		// Feedback is used to adjust the "hotness" of the rumors.
		fRumorList.demote(fResponseBuffer, size);
		return true;
	}

	// ----------------------------------------------------------------------

	private int receiveRumor(SNNode ours, SNNode sender,
			List<Tweet> outsideRumors, ArrayList<Boolean> responseBuffer,
			int protocolID, ICoreInterface application) {

		ListIterator<Tweet> it = outsideRumors.listIterator();
		Linkable sn = fConstraintLinkable.get(ours);

		int bufSize = responseBuffer.size();
		int total = 0;
		int i = 0;

		/**
		 * Goes through the list of "receivable" rumors. We won't decide to
		 * receive a rumor until we know that it might be useful.
		 */
		for (i = 0; it.hasNext() && total < fRumorTransmitSize; i++) {
			Tweet tweet = it.next();
			Boolean wasNew;

			// If the rumor doesn't belong to the profile of a node we know,
			// then we don't care about it.
			Node profileOwner = tweet.profile();
			if (!ours.equals(profileOwner) && !sn.contains(profileOwner)) {
				// Since the hypothesis is that the sender knows which friends
				// we share, we have to make it as if the sender had never
				// sent this rumor.
				//
				// Setting the response flag to true, in this case, gets us
				// exactly that effect.
				wasNew = true;
			}

			else {
				// We know the node.
				// Delivers message to application.
				wasNew = application.receiveTweet(sender, ours, tweet, this);
				// Was it a duplicate?
				if (wasNew) {
					// Nope. Make it a hot rumor.
					fRumorList.add(sn, tweet);
				}
				total++;
			}

			if (i >= bufSize) {
				responseBuffer.add(wasNew);
			} else {
				responseBuffer.set(i, wasNew);
			}
		}

		return i;
	}

	// ----------------------------------------------------------------------

	private void addTweet(Node ours, Tweet tweet) {
		fRumorList.add(fConstraintLinkable.get(ours), tweet);
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
	public void eventDelivered(SNNode sender, SNNode receiver, Tweet tweet,
			boolean duplicate) {
		if (!duplicate) {
			addTweet(receiver, tweet);
		}
	}

	// ----------------------------------------------------------------------

	@Override
	public void tweeted(Tweet tweet) {
		addTweet(tweet.poster, tweet);
	}

	// ----------------------------------------------------------------------

}

// ----------------------------------------------------------------------

/**
 * Rumor list is an auxiliary object which helps the rumor mongering protocol
 * maintain and update its list of rumors.
 */
class RumorList implements Cloneable {

	/**
	 * The rumors we are currently transmitting.
	 */
	private LinkedList<Tweet> fHotRumors = new LinkedList<Tweet>();
	private List<Tweet> fRoHotRumors = Collections.unmodifiableList(fHotRumors);

	/**
	 * Keeps track of destinations for messages.
	 */
	private MultiCounter<Long> fDestinations = new MultiCounter<Long>();

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

	public RumorList(int maxSize, double giveupProbability, Random rnd) {
		fMaxSize = maxSize;
		fGiveupProbability = giveupProbability;
		fRandom = rnd;
	}

	// ----------------------------------------------------------------------

	public void add(Linkable ourSn, Tweet evt) {
		if (addDestinations(ourSn, evt) == 0) {
			return;
		}

		// Hottest rumors are at the END of the list.
		fHotRumors.addLast(evt);
		if (fMaxSize > 0 && fHotRumors.size() > fMaxSize) {
			Tweet discarded = fHotRumors.removeFirst();
			removeDestinations(discarded);
		}
	}

	// ----------------------------------------------------------------------

	public int size() {
		return fHotRumors.size();
	}

	// ----------------------------------------------------------------------

	public List<Tweet> getList() {
		return fRoHotRumors;
	}

	// ----------------------------------------------------------------------

	public void demote(ArrayList<Boolean> mask, int size) {
		ListIterator<Tweet> it = fHotRumors.listIterator(start(size));

		for (int i = 0; it.hasNext() && i < size; i++) {
			// Rumor didn't help.
			if (!mask.get(i)) {
				// Either discards ...
				if (fRandom.nextDouble() < fGiveupProbability) {
					Tweet discarded = it.next();
					it.remove();
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
					Tweet evt = it.next();
					it.previous();
					Tweet previous = it.previous();
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

	private int addDestinations(Linkable ourNeighborhood, Tweet tweet) {
		int size = tweet.destinations();
		int actual = 0;
		for (int i = 0; i < size; i++) {
			Node destination = tweet.destination(i);
			if (ourNeighborhood.contains(destination)
					&& !destination.equals(tweet.poster)) {
				fDestinations.increment(destination.getID());
				actual++;
			}
		}
		return actual;
	}

	// ----------------------------------------------------------------------

	private void removeDestinations(Tweet tweet) {
		int size = tweet.destinations();
		for (int i = 0; i < size; i++) {
			Long destination = tweet.destination(i).getID();
			fDestinations.decrement(destination);
		}
	}

	// ----------------------------------------------------------------------

	public int messagesFor(Node node) {
		return fDestinations.count(node.getID());
	}

	// ----------------------------------------------------------------------
	// Protocol interface.
	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			RumorList cloned = (RumorList) super.clone();
			cloned.fHotRumors = new LinkedList<Tweet>(fHotRumors);
			cloned.fRoHotRumors = Collections
					.unmodifiableList(cloned.fHotRumors);

			return cloned;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}

// ----------------------------------------------------------------------
