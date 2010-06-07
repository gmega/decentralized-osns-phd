package it.unitn.disi.application.demers;

import it.unitn.disi.application.IApplication;
import it.unitn.disi.application.Tweet;
import it.unitn.disi.application.interfaces.IContentExchangeStrategy;
import it.unitn.disi.application.interfaces.IEventObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * Classical rumor mongering algorithm by <a
 * href="http://doi.acm.org/10.1145/41840.41841"> Demers et al. </a>. This is
 * the non-blind version.
 * 
 * @author giuliano
 */
public class DemersRumorMonger implements IContentExchangeStrategy, IEventObserver {

	// ----------------------------------------------------------------------
	// Parameter keys.
	// ----------------------------------------------------------------------
	
	/**
	 * Probability with which a hot rumor will stop being transmitted.
	 */
	private static final String PAR_GIVEUP_PROBABILITY = "giveup_probability";
	
	/**
	 * How many rumors at a time this protocol will transmit.
	 */
	private static final String PAR_TRANSMIT_SIZE = "chunk_size";
	
	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------
	
	private int fRumorTransmitSize;
	
	private final int fSnLinkableId;
	
	private final int fProtocolId;
	
	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	private static final ArrayList<Boolean> fResponseBuffer = new ArrayList<Boolean>();
	
	private RumorList fRumorList;
	
	// ----------------------------------------------------------------------
	
	public DemersRumorMonger(String prefix, int protocolId, int snLinkableId, Random rnd) {
		this(Configuration.getDouble(prefix + "." + PAR_GIVEUP_PROBABILITY),
				Configuration.getInt(prefix + "." + PAR_TRANSMIT_SIZE, Integer.MAX_VALUE),
				protocolId, snLinkableId, rnd); 
	}
	
	// ----------------------------------------------------------------------
	
	public DemersRumorMonger(double giveUp, int rumorTransmitSize,
			int protocolId, int snLinkableId, Random rnd) {
		fRumorList = new RumorList(Integer.MAX_VALUE, giveUp, rnd);
		fRumorTransmitSize = rumorTransmitSize;
		fSnLinkableId = snLinkableId;
		fProtocolId = protocolId;
	}
	
	// ----------------------------------------------------------------------

	/**
	 * Performs a non-blind rumor monger exchange between sender and receiver.
	 * This rumor monger object is assumed to be owned by the sender.
	 */
	public boolean doExchange(Node sender, Node receiver) {
		// Receiver is null. Returns.
		if (receiver == null) {
			return false;
		}

		IApplication application = (IApplication) receiver.getProtocol(fProtocolId);
		DemersRumorMonger rApp = (DemersRumorMonger) application.getAdapter(DemersRumorMonger.class, null);

		// Rumor mongering entails picking a certain number of the
		// "hottest" known rumors and passing them forward.
		int size = rApp.receiveRumor(receiver, sender, fRumorList.getList(),
				fResponseBuffer, fProtocolId, application);

		// Feedback is used to adjust the "hotness" of the rumors.
		fRumorList.demote(fResponseBuffer, size);
		
		return true;
	}
	
	// ----------------------------------------------------------------------
	
	private int receiveRumor(Node ours, Node sender, List<Tweet> outsideRumors,
			ArrayList<Boolean> responseBuffer, int protocolID,
			IApplication application) {

		ListIterator<Tweet> it = outsideRumors.listIterator();
		Linkable sn = (Linkable) ours.getProtocol(fSnLinkableId);

		int bufSize = responseBuffer.size();
		int total = 0;
		int i = 0;

		/**
		 * Goes through the list of "receivable" rumors. We won't decide to
		 * receive a rumor until we know that it might be useful. 
		 */
		for (i = 0; it.hasNext() && total < fRumorTransmitSize; i++) {
			Tweet evt = it.next();
			Boolean wasNew;

			// We don't know the node, so we don't care about this rumor.
			if (!sn.contains(evt.fNode)) {
				// Since the hypothesis is that the sender knows which friends
				// we share, we have to make it as if the sender had never
				// sent this rumor.
				//
				// Setting the response flag to true, in this case, gets us
				// exactly that effect.
				wasNew = true;
			}

			// We know the node. So it means this rumor has been sent.
			else {
				// Delivers message to application.
				wasNew = application.receiveTweet(this, sender, ours, evt);
				// Was it a duplicate?
				if (wasNew) {
					// Nope. Make it a hot rumor.
					fRumorList.add(evt);
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
	
	private void addTweet(Node owner, int sequenceNumber) {
		fRumorList.add(new Tweet(owner, sequenceNumber));
	}
	
	// ----------------------------------------------------------------------
	
	public int throttling() {
		return 1;
	}
	
	// ----------------------------------------------------------------------
	// IEventObserver interface.
	// ----------------------------------------------------------------------
	
	public void tweeted(Node owner, int sequenceNumber) {
		addTweet(owner, sequenceNumber);
	}
	
	// ----------------------------------------------------------------------

	public void eventDelivered(Node sender, Node receiver, Node owner,
			int start, int end) {
		for (int i = (start == -1 ? end : start); i <= end; i++) {
			addTweet(owner, i);
		}
	}
	
	// ----------------------------------------------------------------------
		
	public void duplicateReceived(Node sender, Node receiver, Node owner,
			int start, int end) { }
	
	// ----------------------------------------------------------------------

}

//----------------------------------------------------------------------

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
	 * See {@link DemersRumorMonger#PAR_GIVEUP_PROBABILITY}.
	 */
	private double fGiveupProbability;

	/** Maximum size for the hot rumor list. If the list overgrows this, the "coldest"
	 * rumors start being evicted.
	 */
	private int fMaxSize;

	/** Random number generator. */
	private Random fRandom;
	
	//----------------------------------------------------------------------

	public RumorList(int maxSize, double giveupProbability, Random rnd) {
		fMaxSize = maxSize;
		fGiveupProbability = giveupProbability;
		fRandom = rnd;
	}
	
	//----------------------------------------------------------------------

	public void add(Tweet evt) {
		// Hottest rumors are at the END of the list.
		fHotRumors.addLast(evt);
		if (fMaxSize > 0 && fHotRumors.size() > fMaxSize) {
			fHotRumors.removeFirst();
		}
	}
	
	//----------------------------------------------------------------------

	public int size() {
		return fHotRumors.size();
	}
	
	//----------------------------------------------------------------------

	public List<Tweet> getList() {
		return fRoHotRumors;
	}
	
	//----------------------------------------------------------------------

	public void demote(ArrayList<Boolean> mask, int size) {
		ListIterator<Tweet> it = fHotRumors.listIterator(start(size));

		for (int i = 0; it.hasNext() && i < size; i++) {
			// Rumor didn't help.
			if (!mask.get(i)) {
				// Either discards ...
				if (fRandom.nextDouble() < fGiveupProbability) {
					it.next();
					it.remove();
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
	
	//----------------------------------------------------------------------

	private int start(int size) {
		return Math.max(0, fHotRumors.size() - size);
	}
	
	//----------------------------------------------------------------------
	// Protocol interface.
	//----------------------------------------------------------------------

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
