package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.MulticastService;
import it.unitn.disi.epidemics.NeighborhoodMulticast;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.peersim.IInitializable;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.ArrayList;
import java.util.BitSet;
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
		CDProtocol {
	
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
	
	private static TableWriter fWriter;
	
	private static IMessageVisibility fVisibility;
	
	private static boolean track;

	// ------------------------------------------------------------------------
	// Protocol state.
	// ------------------------------------------------------------------------

	private LinkedList<TreeState> fSoftState = new LinkedList<TreeState>();

	private LinkedList<TreeState> fActiveQueue = new LinkedList<TreeState>();

	private ArrayList<IJoinListener> fListeners = new ArrayList<IJoinListener>();

	private BitSet fSeen = new BitSet();

	private int fSequence;

	private Node fNode;

	public DiscoveryProtocol(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("onehop") int oneHop, @Attribute("twohop") int twoHop,
			@Attribute("membership") int membership,
			@Attribute("multicast") int multicast,
			@Attribute("collect_period") int collect,
			@Attribute("soft_state_timeout") int softStateTimeout) {
		this(PeersimUtils.selfPid(prefix), oneHop, twoHop, membership,
				multicast, collect, softStateTimeout, null);
	}

	public DiscoveryProtocol(int selfPid, int oneHop, int twoHop,
			int membership, int multicast, int collect, int softStateTimeout,
			TableWriter writer) {
		fOneHop = oneHop;
		fTwoHop = twoHop;
		fMembership = membership;
		fMulticast = multicast;
		fCollectPeriod = collect;
		fSelfPid = selfPid;
		fSoftStateTimeout = softStateTimeout;
		fVisibility = new NeighborhoodMulticast(fTwoHop);
		fWriter = writer;
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
	}

	// ------------------------------------------------------------------------

	private void collectDescriptors(Node node) {
		Linkable membership = (Linkable) node.getProtocol(fMembership);
		Linkable onehop = onehop();
		int members = membership.degree();
		for (int i = 0; i < members; i++) {
			Node candidate = membership.getNeighbor(i);
			if (onehop.contains(candidate)) {
				fSeen.set(PeersimUtils.indexOf(candidate, onehop));
			}
		}
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
		// XXX maybe refresh soft state of the tree?
		if (duplicate) {
			return;
		}

		AdvertisementMessage adv = (AdvertisementMessage) message;

		// Tries to add, if useful:
		// 1. ourselves;
		adv.add(fNode);
		// 2. neighbors we've seen;
		fromCurrentView(adv);
		// 3. neighbors in the peersampling view.
		fromPeerSampling(receiver, adv);

		// Creates tree soft state entry, but don't activate it yet.
		create(sender, adv.originator(), adv.sequenceNumber(), adv.bitset());
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
		TreeState state = lookup(msg.originator(), msg.sequenceNumber());
		if (state == null) {
			state = treeShortcut(msg.originator(), msg.sequenceNumber(),
					message.bitset());
		}
		enqueue(state);
	}

	// ------------------------------------------------------------------------

	private void aggregate() {
		TreeState tree = peek();
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

		if (homingNode != null) {
			DiscoveryProtocol protocol = (DiscoveryProtocol) homingNode
					.getProtocol(fSelfPid);
			protocol.home(fNode, tree);
			// No longer dirty.
			tree.clear();
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * A node is contacting ours with some advertisement results.
	 * 
	 * @param sender
	 * @param message
	 */
	protected void home(Node sender, TreeState homed) {
		// TODO log how many bytes were transferred.
		// Are we the final destination for this message?
		if (homed.originator().equals(fNode)) {
			// Merges all into our view.
			int transferred = homed.transfer(fSeen);
			if (track) {
				track(TRANSFER, sender.getID(), fNode.getID(), transferred);
			}
			return;
		}

		// We're not, but this message starts our aggregation phase
		// for this tree.
		// Do we have some previous state?
		TreeState tree = lookup(homed.originator(), homed.sequenceNumber());
		boolean activate = false;
		if (tree != null) {
			// Yes we do, refreshes the soft state.
			int transferred = tree.refresh(homed);
			if (track) {
				track(TRANSFER, sender.getID(), fNode.getID(), transferred);
			}
			activate = transferred != 0;
		} else {
			// No we don't, need to (re-)creates the soft state. Note that
			// when we have to re-create the soft state we shortcut the
			// tree to the head. Way around this would be to embed the
			// dissemination path into the forward message.
			activate = true;
			tree = treeShortcut(homed.originator(), homed.sequenceNumber(),
					homed.bitset());
		}

		if (activate && !tree.isQueued()) {
			tree.setQueued(true);
			fActiveQueue.addLast(tree);
		}
	}

	// ------------------------------------------------------------------------

	private TreeState treeShortcut(Node originator, int sequenceNumber,
			BitSet bootstrap) {
		System.out.println("DP: Tree shortcutted.");
		return create(null, originator, sequenceNumber, bootstrap);
	}

	// ------------------------------------------------------------------------

	private void discardStaleTrees() {
		Iterator<TreeState> it = fSoftState.iterator();
		while (it.hasNext()) {
			TreeState state = it.next();
			if (state.age() >= fSoftStateTimeout) {
				if (track) {
					track(SF_DISCARD, state.originator(), fNode.getID(), state.sequenceNumber());
				}
				// Soft-state cleanup.
				it.remove();
				// If dirty, however, enqueue it so we send the
				// information down the tree before we discard it.
				enqueue(state);
			}
		}
	}

	// ------------------------------------------------------------------------

	public void joinDone(IGossipMessage msg, JoinTracker tracker) {
		for (IJoinListener listener : fListeners) {
			listener.joinDone((AdvertisementMessage) msg, tracker.copies());
		}
	}

	@Override
	public void localDelivered(IGossipMessage message) {
		// Makes no sense in our case.
	}

	public int size() {
		return fSeen.cardinality();
	}

	public double success() {
		return size() / ((double) onehop().degree());
	}

	public boolean canContact(int idx) {
		return fSeen.get(idx);
	}

	private void advertise() {
		// Multicasts an advertisement to our group.
		MulticastService mcast = (MulticastService) fNode
				.getProtocol(fMulticast);
		AdvertisementMessage msg = new AdvertisementMessage(fNode, fSequence++,
				fOneHop, new JoinTracker(this), fVisibility);
		mcast.multicast(msg);
	}

	// ------------------------------------------------------------------------
	// IInitializable interface.
	// ------------------------------------------------------------------------

	@Override
	public void initialize(Node node) {
		fNode = node;
	}

	// ------------------------------------------------------------------------

	@Override
	public void reinitialize() {
		advertise();
	}

	// ------------------------------------------------------------------------
	// Soft state management.
	// ------------------------------------------------------------------------

	private TreeState create(Node parent, Node originator, int sequenceNumber,
			BitSet bootstrap) {
		TreeState state = lookup(originator, sequenceNumber);
		if (state == null) {
			state = new TreeState(parent, originator, sequenceNumber, bootstrap);
			fSoftState.add(state);
			if (track) {
				track(SF_CREATE, parent.getID(), fNode.getID(), sequenceNumber);
			}
		}
		return state;
	}

	// ------------------------------------------------------------------------

	private TreeState lookup(Node originator, int sequenceNumber) {
		for (TreeState candidate : fSoftState) {
			if (candidate.isTree(originator, sequenceNumber)) {
				return candidate;
			}
		}
		return null;
	}

	// ------------------------------------------------------------------------
	// Active queue management.
	// ------------------------------------------------------------------------

	public void enqueue(TreeState state) {
		if (state.isQueued()) {
			return;
		}
		fActiveQueue.addLast(state);
		state.setQueued(true);
	}

	// ------------------------------------------------------------------------

	public TreeState dequeue() {
		TreeState state = fActiveQueue.removeFirst();
		state.setQueued(false);
		return state;
	}

	// ------------------------------------------------------------------------

	public TreeState peek() {
		return fActiveQueue.getFirst();
	}

	// ------------------------------------------------------------------------
	// Methods for collecting descriptors into passing messages.
	// ------------------------------------------------------------------------

	private void fromCurrentView(AdvertisementMessage adv) {
		Linkable lnk = onehop();
		for (int i = fSeen.nextSetBit(0); i != -1; i = fSeen.nextSetBit(i + 1)) {
			adv.add(lnk.getNeighbor(i));
		}
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
	
	public int pid() {
		return fSelfPid;
	}

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

	// ------------------------------------------------------------------------
	// Cloneable requirements.
	// ------------------------------------------------------------------------

	public Object clone() {
		try {
			DiscoveryProtocol theClone = (DiscoveryProtocol) super.clone();
			theClone.fSeen = new BitSet();
			theClone.fSeen.or(fSeen);
			theClone.fSoftState = new LinkedList<TreeState>();
			theClone.fListeners = new ArrayList<IJoinListener>();
			theClone.fActiveQueue = new LinkedList<TreeState>();
			return theClone;
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
}

class TreeState {

	private Node fParent;

	private Node fOriginator;

	private final int fSequence;

	private final BitSet fAggregate;

	private int fAge;

	private boolean fDirty;

	private boolean fActive;

	private boolean fTryParent;

	public TreeState(Node parent, Node originator, int sequence,
			BitSet bootstrap) {
		fAggregate = new BitSet();
		fOriginator = originator;
		fParent = parent;
		fSequence = sequence;
		fTryParent = (parent != null);
		refresh(bootstrap);
	}

	public int refresh(TreeState other) {
		return this.refresh(other.fAggregate);
	}

	private int refresh(BitSet state) {
		fAge = CommonState.getIntTime();
		int transferred = transfer(state, fAggregate);
		fDirty = (transferred != 0);
		return transferred;
	}

	public int transfer(BitSet other) {
		return transfer(fAggregate, other);
	}

	private int transfer(BitSet source, BitSet target) {
		int before = target.cardinality();
		target.or(source);
		return target.cardinality() - before;
	}

	BitSet bitset() {
		return fAggregate;
	}

	public int age() {
		return CommonState.getIntTime() - fAge;
	}

	public int sequenceNumber() {
		return fSequence;
	}

	public Node parent() {
		return fParent;
	}

	public Node originator() {
		return fOriginator;
	}

	public boolean shouldTryParent() {
		return fTryParent;
	}

	public void parentFailed() {
		fTryParent = false;
	}

	public boolean isTree(Node originator, int sequence) {
		return sequence == this.fSequence && originator == this.fOriginator;
	}

	public boolean isDirty() {
		return fDirty;
	}

	public void clear() {
		fDirty = false;
	}

	public boolean isQueued() {
		return fActive;
	}

	public void setQueued(boolean state) {
		fActive = state;
	}
}
