package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.analysis.online.NodeStatistic;
import it.unitn.disi.epidemics.IApplicationInterface;
import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IEventStorage;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.newscasting.internal.demers.IDestinationTracker.Result;
import it.unitn.disi.newscasting.internal.demers.RumorList.IDemotionObserver;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.ProtocolReference;
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
	
	public static final String PAR_STATS = "statistics";

	// ----------------------------------------------------------------------
	// Parameter storage.
	// ----------------------------------------------------------------------

	private int fRumorTransmitSize;

	private double fGiveup;

	private Random fRandom;

	private final int fProtocolId;

	private Node fNode;

	private final IReference<NodeStatistic> fStats;

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	private static final ArrayList<Boolean> fResponseBuffer = new ArrayList<Boolean>();

	private RumorList fRumorList;

	private IDestinationTracker fTracker;

	// ----------------------------------------------------------------------

	public DemersRumorMonger(IResolver resolver, String prefix, int protocolId,
			Node source, Random rnd, boolean unitOptimized) {
		this(resolver.getDouble(prefix, PAR_GIVEUP_PROBABILITY), resolver
				.getInt(prefix, PAR_TRANSMIT_SIZE), protocolId,
				new ProtocolReference<NodeStatistic>(resolver.getInt(prefix,
						PAR_STATS)), source, (Linkable) source
						.getProtocol(resolver.getInt(prefix, PAR_LINKABLE)),
				rnd, unitOptimized);
	}

	// ------------------------------------------------------------------------

	public DemersRumorMonger(double giveUp, int rumorTransmitSize,
			int protocolId, IReference<NodeStatistic> stats, Node source,
			Linkable constraintLinkable, Random rnd, boolean unitOptimized) {
		fRumorTransmitSize = rumorTransmitSize;
		fProtocolId = protocolId;
		fGiveup = giveUp;
		fRandom = rnd;
		fNode = source;
		fRumorList = new RumorList(Integer.MAX_VALUE, fGiveup, fRandom,
				new IDemotionObserver() {
					@Override
					public void dropped(IGossipMessage msg) {
						fTracker.drop(msg);
					}

					@Override
					public void demoted(IGossipMessage msg) {
					}
				});

		fTracker = unitOptimized ? new UEOptimizedDestinationTracker(
				constraintLinkable) : new CountingDestinationTracker(
				constraintLinkable);

		fStats = stats;
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
		int transferred = 0;
		/**
		 * Goes through the list of "receivable" rumors. We won't decide to
		 * receive a rumor until we know that it might be useful.
		 */
		for (i = 0; it.hasNext() && total < fRumorTransmitSize; i++) {
			IGossipMessage message = it.next();
			boolean isNew = true;
			if (!message.originator().equals(ours)) {
				IGossipMessage ourMessage = null;
				// Is this rumor already known?
				IEventStorage storage = application.storage();
				isNew = !storage.contains(message);
				Result result = null;
				if (isNew) {
					// Nope. Asks the destination tracker to tell us
					// whether it's useful or not.
					result = fTracker.track(message);
					if (result != Result.no_intersection) {
						message.forwarded(sender, ours);
						ourMessage = message.cloneIfNeeded();
					}

					if (result == Result.forward) {
						fRumorList.add(ours, ourMessage);
					}

				} else {
					ourMessage = storage.retrieve(message.originator(),
							message.sequenceNumber());
					if (ourMessage == null) {
						throw new InternalError();
					}
				}

				// Delivers only if there's something to deliver.
				if (ourMessage != null) {
					application.deliver(sender, ours, ourMessage, this);
					transferred += message.sizeOf();
					total++;
				}

				// Finally, emulates the drop if we're not adding because
				// we won't send back to originator.
				if (result == Result.originator_only) {
					ourMessage.dropped(fNode);
				}
			}

			// Note that if we don't deliver the message, wasNew will be true,
			// which means the rumor won't be demoted at the sender and it will
			// be as if he never sent it.
			responseBufferAppend(responseBuffer, i, isNew);
		}
		
		if (transferred != 0 && fStats != null) {
			NodeStatistic sendStats = fStats.get(sender);
			NodeStatistic recvStats = fStats.get(ours);
			sendStats.messageSent(transferred);
			recvStats.messageReceived(transferred);
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

	private void addTweet(Node sender, IGossipMessage tweet) {
		if (fTracker.track(tweet) == Result.forward) {
			fRumorList.add(fNode, tweet);
		}
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
	public boolean canSelect(Node source, Node target) {
		return fTracker.count(target) != 0;
	}

	// ----------------------------------------------------------------------

	public Node selected(Node source, Node node) {
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
			addTweet(sender, tweet);
		}
	}

	// ----------------------------------------------------------------------

	@Override
	public void localDelivered(IGossipMessage message) {
		addTweet(fNode, message);
	}

	// ----------------------------------------------------------------------

}

// ----------------------------------------------------------------------
