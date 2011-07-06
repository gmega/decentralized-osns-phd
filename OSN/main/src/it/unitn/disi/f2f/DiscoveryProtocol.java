package it.unitn.disi.f2f;

import it.unitn.disi.analysis.online.NodeStatistic;
import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.MulticastService;
import it.unitn.disi.epidemics.NeighborhoodMulticast;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.peersim.BitSetNeighborhood;
import it.unitn.disi.utils.peersim.IInitializable;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link DiscoveryProtocol}'s initial idea is to allow a node joining the
 * network to discover its one hop neighbors by assuming that the search for
 * these neighbors occurs over some other graph.
 * 
 * We basically propagate an {@link AdvertisementMessage} over the search graph
 * (e.g. one built by peer sampling) and, at each hop, nodes try to fill in the
 * {@link AdvertisementMessage} with neighbors from the originator that they
 * might know (see {@link #delivered(SNNode, SNNode, IGossipMessage, boolean)}).
 * 
 * @author giuliano
 */
@AutoConfig
@StructuredLog(key = "DP", fields = { "event", "sender", "receiver", "extra" })
public class DiscoveryProtocol implements IEventObserver, IInitializable,
		CDProtocol, Linkable {

	// ------------------------------------------------------------------------
	// Constants for event tracking.
	// ------------------------------------------------------------------------

	public static final String SF_CREATE = "S";

	public static final String SF_DISCARD = "D";

	public static final String LEAF = "L";

	public static final String TRANSFER = "T";

	// ------------------------------------------------------------------------
	// Parameter storage.
	// ------------------------------------------------------------------------

	private static int fOneHop;

	private static int fTwoHop;

	private static int fMembership;

	private static int fMulticast;

	private static int fSelfPid;

	private static int fCollectPeriod;

	private static int fSoftStateTimeout;

	private static IReference<NodeStatistic> fStats;

	private static TableWriter fWriter;

	private static IMessageVisibility fVisibility;

	private static boolean track;

	// ------------------------------------------------------------------------
	// Protocol state.
	// ------------------------------------------------------------------------

	private LinkedList<AggregationTreeState> fSoftState = new LinkedList<AggregationTreeState>();

	private LinkedList<AggregationTreeState> fActiveQueue = new LinkedList<AggregationTreeState>();

	private ArrayList<IJoinListener> fListeners = new ArrayList<IJoinListener>();

	private BitSetNeighborhood fDescriptors;

	private int fSequence;

	private int fPSHits;

	private int fDiscoveryHits;
	
	private int fDiscoveryWaste;
	
	private int fAccidentalHits;

	private Node fNode;

	public DiscoveryProtocol(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("onehop") int oneHop, @Attribute("twohop") int twoHop,
			@Attribute("membership") int membership,
			@Attribute("multicast") int multicast,
			@Attribute("collect_period") int collect,
			@Attribute("soft_state_timeout") int softStateTimeout,
			@Attribute("statistics") int stats) {
		this(PeersimUtils.selfPid(prefix), oneHop, twoHop, membership,
				multicast, collect, softStateTimeout,
				new ProtocolReference<NodeStatistic>(stats), null);
	}

	public DiscoveryProtocol(int selfPid, int oneHop, int twoHop,
			int membership, int multicast, int collect, int softStateTimeout,
			IReference<NodeStatistic> stats, TableWriter writer) {
		fOneHop = oneHop;
		fTwoHop = twoHop;
		fMembership = membership;
		fMulticast = multicast;
		fCollectPeriod = collect;
		fSelfPid = selfPid;
		fSoftStateTimeout = softStateTimeout;
		fVisibility = new NeighborhoodMulticast(fTwoHop);
		fWriter = writer;
		fStats = stats;
		track = fWriter != null;
	}

	// ------------------------------------------------------------------------

	@Override
	public void nextCycle(Node node, int protocolID) {
		// This doesn't entail any communication.
		if (shouldCollectDescriptors()) {
			collectDescriptors(node);
		}

		// Homes messages.
		if (!fActiveQueue.isEmpty()) {
			aggregate();
		}

		// Discards soft state.
		discardStaleTrees();
		
		// Collects garbage from view.
		collectGarbage();
	}

	// ------------------------------------------------------------------------

	private void collectDescriptors(Node node) {
		Linkable membership = (Linkable) node.getProtocol(fMembership);
		Linkable onehop = onehop();
		int members = membership.degree();
		for (int i = 0; i < members; i++) {
			Node candidate = membership.getNeighbor(i);
			if (onehop.contains(candidate)
					&& fDescriptors.addNeighbor(candidate)) {
				fPSHits++;
			}
		}
	}
	
	// ------------------------------------------------------------------------
	
	private void collectGarbage() {
		
	}

	// ------------------------------------------------------------------------

	private boolean shouldCollectDescriptors() {
		return CommonState.getTime() % fCollectPeriod == 0;
	}

	// ------------------------------------------------------------------------
	// Dissemination tree.
	// ------------------------------------------------------------------------

	@Override
	public void delivered(SNNode sender, SNNode receiver,
			IGossipMessage message, boolean duplicate) {
		// We don't care about duplicates in this context.
		if (duplicate) {
			return;
		}

		AdvertisementMessage adv = (AdvertisementMessage) message;

		// We won't send back what was already in the message.
		BitSetNeighborhood contents = new BitSetNeighborhood(adv.neighborhood());

		// Tries to add, if useful:
		// 1. ourselves;
		adv.add(fNode);
		// 2. neighbors we've seen;
		fromCurrentView(adv);
		// 3. neighbors in the peersampling view.
		fromPeerSampling(receiver, adv);

		// Clones the message state again, and subtracts what we had before.
		BitSetNeighborhood toSend = new BitSetNeighborhood(adv.neighborhood());
		toSend.setDifference(contents);

		// Creates tree soft state entry, but don't activate it yet.
		create(sender, adv, contents, toSend);
	}

	// ------------------------------------------------------------------------

	public void dropped(IGossipMessage msg) {
		// When a message is dropped, this might precipitate the
		// beginning of the aggregation phase if we're a leaf
		// node in the dissemination tree.
		AdvertisementMessage message = (AdvertisementMessage) msg;
		if (message.wasForwarded() || msg.originator().equals(fNode)) {
			return;
		}

		if (track) {
			track(LEAF, "none", fNode.getID(), "none");
		}

		// We're leaf, start sending stuff back.
		AggregationTreeState state = lookup(msg.originator(),
				msg.sequenceNumber());
		if (state == null) {
			state = treeShortcut(message,
					new BitSetNeighborhood(fDescriptors.linkable()),
					new BitSetNeighborhood(message.neighborhood()));
		}
		enqueue(state);
	}

	// ------------------------------------------------------------------------

	private void aggregate() {
		AggregationTreeState tree = peek();
		Node homingNode = null;

		// Tries to home it into the tree parent, if we still
		// know who that is and it is still on-line.
		if (tree.shouldTryParent()) {
			homingNode = tree.parent();
			if (!homingNode.isUp()) {
				tree.parentFailed();
				return;
			}
		}
		// Otherwise tries to home it directly to originator, if he's still
		// in the network.
		else if (tree.originator().isUp()) {
			homingNode = tree.originator();
		}

		// Either way, if we made this far we remove the message from the active
		// queue.
		dequeue();

		if (homingNode != null && homingNode.isUp()) {
			DiscoveryProtocol protocol = (DiscoveryProtocol) homingNode
					.getProtocol(fSelfPid);
			fStats.get(fNode).messageSent(tree.sizeOf());
			protocol.home(fNode, tree);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * A node is contacting ours with some advertisement results.
	 * 
	 * @param sender
	 * @param message
	 */
	protected void home(Node sender, AggregationTreeState homed) {
		// Merges descriptors into our view. This possibly won't result
		// in anything being added to our view.
		int length = homed.pendingDescriptors();
		int effective = homed.merge(fDescriptors);
		fDiscoveryHits += effective;
		fDiscoveryWaste += (length - effective);
		
		// Tracks how many bytes we just got.
		fStats.get(fNode).messageReceived(homed.sizeOf());

		// If we were the final destination for this message, we're done.
		if (homed.originator().equals(fNode)) {
			homed.done();
			if (track) {
				track(TRANSFER, sender.getID(), fNode.getID(), length);
			}
			return;
		}
		
		// Anything we got in terms of new descriptors has been
		// by accident.
		fAccidentalHits += effective;

		// Otherwise we need to push the message down the tree.
		// Do we have some previous state?
		AggregationTreeState tree = lookup(homed.originator(),
				homed.sequenceNumber());
		boolean created = false;
		// No we don't, need to (re-)create the soft state. Note that
		// when we have to re-create the soft state we shortcut the
		// tree to the head. Way around this would be to embed the
		// dissemination path into the forward message.
		if (tree == null) {
			created = true;
			tree = treeShortcut(
					message(homed.originator(), homed.sequenceNumber()),
					new BitSetNeighborhood(fDescriptors.linkable()),
					new BitSetNeighborhood(fDescriptors.linkable()));
		}

		tree.mergeAndForward(homed);

		if (track) {
			track(TRANSFER, sender.getID(), fNode.getID(), length);
		}

		if (created || (length != 0 && !tree.isQueued())) {
			tree.setQueued(true);
			fActiveQueue.addLast(tree);
		}
	}

	// ------------------------------------------------------------------------

	private AggregationTreeState treeShortcut(AdvertisementMessage message,
			BitSetNeighborhood filter, BitSetNeighborhood push) {
		System.out.println("DP: Tree shortcutted.");
		return create(null, message, filter, push);
	}

	// ------------------------------------------------------------------------

	private void discardStaleTrees() {
		Iterator<AggregationTreeState> it = fSoftState.iterator();
		while (it.hasNext()) {
			AggregationTreeState state = it.next();
			if (state.age() >= fSoftStateTimeout) {
				if (track) {
					track(SF_DISCARD, state.originator(), fNode.getID(),
							state.sequenceNumber());
				}
				// Soft-state cleanup.
				it.remove();
				// If dirty, however, enqueue it so we send the
				// information down the tree before we discard it.
				if (state.isDirty()) {
					enqueue(state);
				}
				state.collected();
			}
		}
	}

	// ------------------------------------------------------------------------

	public void joinStart(IGossipMessage msg, JoinTracker tracker) {
		Iterator<IJoinListener> it = fListeners.iterator();
		while (it.hasNext()) {
			IJoinListener listener = it.next();
			listener.joinStarted((AdvertisementMessage) msg);
		}
	}

	// ------------------------------------------------------------------------

	public void joinDone(IGossipMessage msg, JoinTracker tracker) {
		Iterator<IJoinListener> it = fListeners.iterator();
		while (it.hasNext()) {
			IJoinListener listener = it.next();
			if (listener.joinDone((AdvertisementMessage) msg, tracker)) {
				it.remove();
			}
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public void localDelivered(IGossipMessage message) {
		// Makes no sense in our case.
	}

	// ------------------------------------------------------------------------

	public double success() {
		return degree() / ((double) onehop().degree());
	}

	// ------------------------------------------------------------------------
	// IInitializable interface.
	// ------------------------------------------------------------------------

	@Override
	public void initialize(Node node) {
		fNode = node;
		fDescriptors = new BitSetNeighborhood(
				(Linkable) node.getProtocol(fOneHop));
	}

	// ------------------------------------------------------------------------

	@Override
	public void reinitialize() {
		advertise();
	}

	// ------------------------------------------------------------------------

	private void advertise() {
		// Multicasts an advertisement to our group.
		MulticastService mcast = (MulticastService) fNode
				.getProtocol(fMulticast);
		AdvertisementMessage msg = new AdvertisementMessage(fNode, fSequence++,
				fOneHop, new JoinTracker(this), fVisibility);
		mcast.multicast(msg);
		joinStart(msg, msg.tracker());
	}

	// ------------------------------------------------------------------------
	// Soft state management.
	// ------------------------------------------------------------------------

	private AggregationTreeState create(Node parent,
			AdvertisementMessage parentMsg, BitSetNeighborhood filter,
			BitSetNeighborhood push) {
		AggregationTreeState state = lookup(parentMsg.originator(),
				parentMsg.sequenceNumber());
		if (state == null) {
			state = new AggregationTreeState(parent, filter, push, parentMsg);
			fSoftState.add(state);
			if (track) {
				track(SF_CREATE, parent.getID(), fNode.getID(),
						parentMsg.sequenceNumber());
			}
		}
		parentMsg.tracker().joinAggregation();
		return state;
	}

	// ------------------------------------------------------------------------

	private AggregationTreeState lookup(Node originator, int sequenceNumber) {
		for (AggregationTreeState candidate : fSoftState) {
			if (candidate.isTree(originator, sequenceNumber)) {
				return candidate;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------------

	private AdvertisementMessage message(Node originator, int sequence) {
		// XXX this needs to be improved.
		MulticastService mcast = (MulticastService) fNode
				.getProtocol(fMulticast);
		AdvertisementMessage message = (AdvertisementMessage) mcast.storage()
				.retrieve(originator, sequence);
		if (message == null) {
			throw new IllegalStateException();
		}
		return message;
	}

	// ------------------------------------------------------------------------
	// Active queue management.
	// ------------------------------------------------------------------------

	public void enqueue(AggregationTreeState state) {
		if (state.isQueued()) {
			return;
		}
		fActiveQueue.addLast(state);
		state.setQueued(true);
	}

	// ------------------------------------------------------------------------

	public AggregationTreeState dequeue() {
		AggregationTreeState state = fActiveQueue.removeFirst();
		state.setQueued(false);
		return state;
	}

	// ------------------------------------------------------------------------

	public AggregationTreeState peek() {
		return fActiveQueue.getFirst();
	}

	// ------------------------------------------------------------------------
	// Methods for collecting descriptors into passing messages.
	// ------------------------------------------------------------------------

	private void fromCurrentView(AdvertisementMessage adv) {
		adv.neighborhood().addAll(fDescriptors);
	}

	// ------------------------------------------------------------------------

	private void fromPeerSampling(Node ours, AdvertisementMessage adv) {
		Linkable membership = (Linkable) ours.getProtocol(fMembership);
		int members = membership.degree();
		for (int i = 0; i < members; i++) {
			Node member = (Node) membership.getNeighbor(i);
			adv.add(member);
		}
	}

	// ------------------------------------------------------------------------

	public Linkable onehop() {
		return (Linkable) fNode.getProtocol(fOneHop);
	}

	// ------------------------------------------------------------------------

	public int pid() {
		return fSelfPid;
	}

	// ------------------------------------------------------------------------

	private void track(String type, Object sender, Object receiver, Object value) {
		fWriter.set("event", type);
		fWriter.set("sender", sender.toString());
		fWriter.set("receiver", receiver.toString());
		fWriter.set("extra", value.toString());
		fWriter.emmitRow();
	}

	// ------------------------------------------------------------------------
	// Event management.
	// ------------------------------------------------------------------------

	public void addJoinListener(IJoinListener listener) {
		fListeners.add(listener);
	}

	public void removeJoinListener(IJoinListener listener) {
		fListeners.remove(listener);
	}

	// ------------------------------------------------------------------------
	// Linkable interface.
	// ------------------------------------------------------------------------

	@Override
	public int degree() {
		return fDescriptors.degree();
	}

	// ------------------------------------------------------------------------

	@Override
	public Node getNeighbor(int i) {
		return fDescriptors.getNeighbor(i);
	}

	// ------------------------------------------------------------------------

	@Override
	public boolean addNeighbor(Node neighbour) {
		return fDescriptors.addNeighbor(neighbour);
	}

	// ------------------------------------------------------------------------

	@Override
	public boolean contains(Node neighbor) {
		return fDescriptors.contains(neighbor);
	}

	// ------------------------------------------------------------------------

	@Override
	public void onKill() {
		fDescriptors = null;
	}

	// ------------------------------------------------------------------------

	@Override
	public void pack() {
	}

	// ------------------------------------------------------------------------
	// Inspection methods.
	// ------------------------------------------------------------------------
	/**
	 * @return nodes which have been seen and are actually alive.
	 */
	public int seen() {
		int seen = 0;
		for (Node node : fDescriptors) {
			if (node.isUp()) {
				seen++;
			}
		}
		return seen;
	}

	// ------------------------------------------------------------------------

	/**
	 * @return nodes which are alive but haven't been seen.
	 */
	public int unseen() {
		Linkable lnk = onehop();
		int degree = lnk.degree();
		int unseen = 0;
		for (int i = 0; i < degree; i++) {
			Node neighbor = lnk.getNeighbor(i);
			if (neighbor.isUp() && !fDescriptors.contains(neighbor)) {
				unseen++;
			}
		}
		return unseen;
	}

	// ------------------------------------------------------------------------

	/**
	 * @return nodes which have been seen but are dead.
	 */
	public int stale() {
		int stale = 0;
		for (Node node : fDescriptors) {
			if (!node.isUp()) {
				stale++;
			}
		}
		return stale;
	}
	
	// ------------------------------------------------------------------------
	
	public int membershipHits() {
		return fPSHits;
	}
	
	// ------------------------------------------------------------------------
	
	public int joinHits() {
		return fDiscoveryHits;
	}
	
	// ------------------------------------------------------------------------
	
	public int accidentalHits() {
		return fAccidentalHits;
	}
	
	// ------------------------------------------------------------------------
	
	public int joinWaste() {
		return fDiscoveryWaste;
	}
	
	// ------------------------------------------------------------------------

	// ------------------------------------------------------------------------
	// Cloneable requirements.
	// ------------------------------------------------------------------------

	public Object clone() {
		try {
			DiscoveryProtocol theClone = (DiscoveryProtocol) super.clone();
			theClone.fDescriptors = null;
			theClone.fSoftState = new LinkedList<AggregationTreeState>();
			theClone.fListeners = new ArrayList<IJoinListener>();
			theClone.fActiveQueue = new LinkedList<AggregationTreeState>();
			return theClone;
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
}
