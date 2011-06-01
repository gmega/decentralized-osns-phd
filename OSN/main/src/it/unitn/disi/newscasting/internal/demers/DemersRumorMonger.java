package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.ArrayList;
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
	public static final String PAR_LINKABLE = "constraint_linkable";

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
			Node source, Random rnd, boolean unitOptimized) {
		this(resolver.getDouble(prefix, PAR_GIVEUP_PROBABILITY), resolver
				.getInt(prefix, PAR_TRANSMIT_SIZE), protocolId, source,
				(Linkable) source.getProtocol(resolver.getInt(prefix,
						PAR_LINKABLE)), rnd, unitOptimized);
	}

	// ------------------------------------------------------------------------

	public DemersRumorMonger(double giveUp, int rumorTransmitSize,
			int protocolId, Node source, Linkable constraintLinkable,
			Random rnd, boolean unitOptimized) {
		fRumorTransmitSize = rumorTransmitSize;
		fProtocolId = protocolId;
		fGiveup = giveUp;
		fRandom = rnd;
		fRumorList = unitOptimized ? new UEOptimizedRumorList(
				Integer.MAX_VALUE, fGiveup, constraintLinkable, fRandom)
				: new CountingRumorList(Integer.MAX_VALUE, fGiveup,
						constraintLinkable, fRandom);
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
		return (fRumorList.size == 0) ? ActivityStatus.QUIESCENT
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
		fRumorList.dropAll(source);
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
