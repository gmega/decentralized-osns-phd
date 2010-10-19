package it.unitn.disi.sps.newscast;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.IRebootable;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.sps.newscast.View.BufferHandler;
import it.unitn.disi.sps.selectors.ISelector;
import it.unitn.disi.sps.selectors.RandomSelector;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.collections.IExchanger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Random;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.config.FastConfig;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

/**
 * "Social peer sampling" arised from the desire to constrain gossip exchanges
 * to a social network. More generally, this peer sampling service can be used
 * to constrain gossip exchanges to any underlying graph, as long as it is
 * "reasonably" static.
 * 
 * The extent to which conformance to the underlying graph is affected by its
 * dynamism is still to be investigated, but if for sure depends on how the H
 * parameter (healer) is set.
 * 
 * This protocol draws on previous ideas from Jel�sity et. al on <a
 * href="http://doi.acm.org/10.1145/1275517.1275520"> gossip-based peer
 * sampling</a>.
 * 
 * @author giuliano
 */
public class NewscastSN implements IPeerSelector, IRebootable,
		CDProtocol, IDynamicLinkable {

	// --------------------------------------------------------------------------
	// Parameters
	// --------------------------------------------------------------------------

	/**
	 * The cache size (partial view) of the gossip protocol.
	 * 
	 * @config
	 */
	private static final String PAR_CACHE = "cache";

	/**
	 * The "healer" parameter (number of old descriptors to be discarded at each
	 * exchange).
	 * 
	 * @config
	 */
	private static final String PAR_HEALER = "healer";

	/**
	 * The "swapper" parameter (number of propagated descriptors to be discarded
	 * after each exchange).
	 * 
	 * @config
	 */
	private static final String PAR_SWAPPER = "swapper";
	
	/**
	 * If present, causes the passive side of the protocol to not inject fresh
	 * descriptors.
	 */
	private static final String PAR_ACTIVE_INJECTION = "activeInjectionOnly";

	/**
	 * Debug flag -- for development purposes. Should be set to off.
	 */
	private static final String PAR_DEBUG = "debug";
	
	// --------------------------------------------------------------------------
	// Storage for peersim parameters.
	// --------------------------------------------------------------------------

	/**
	 * Value for the "swapper" parameter. Obtained from {@link #PAR_SWAPPER}.
	 */
	private int fS;

	/**
	 * Value for the "healer" parameter. Obtained from {@value #PAR_HEALER}.
	 */
	private int fH;
	
	/**
	 * Value for the "active injection only" parameter. Obtained from {@value #PAR_ACTIVE_INJECTION}
	 */
	private boolean fActiveInjectionOnly;

	/**
	 * Value for the "debug" flag. Obtained from {@link #PAR_DEBUG}.
	 */
	private boolean fDebug;

	// --------------------------------------------------------------------------
	// Other fields.
	// --------------------------------------------------------------------------
	/**
	 * Timestamp of the last change to the view.
	 */
	private int fLastChange;

	private Random fRandom;

	// --------------------------------------------------------------------------
	// Fields which need special procedures for cloning.
	// --------------------------------------------------------------------------
	/**
	 * Reference to the internal view object.
	 */
	private View fView;

	/**
	 * Manages the peer selection queue.
	 */
	private QueueManager fQueue;

	/**
	 * Pluggable algorithm for selecting peers for the peersampling exchanges.
	 */
	private ISelector fSelector;

	/**
	 * Selection filter for constraining peer exchanges.
	 */
	private ISelectionFilter fFilter;

	// --------------------------------------------------------------------------
	// Initialization
	// --------------------------------------------------------------------------

	/** PeerSim-specific constructor. */
	public NewscastSN(String s) {
		this(Configuration.getInt(s + "." + PAR_CACHE), Configuration.getInt(s
				+ "." + PAR_HEALER), Configuration
				.getInt(s + "." + PAR_SWAPPER), CommonState.r, Configuration
				.contains(s + "." + PAR_DEBUG),
				Configuration.contains(s + "." + PAR_ACTIVE_INJECTION));
	}

	// --------------------------------------------------------------------------

	/** "Regular" constructor (doesn't require PeerSim to be running). */
	NewscastSN(int cacheSize, int h, int s, Random r, boolean debug, boolean activeInjectionOnly) {
		this.init(cacheSize, h, s, r, debug, activeInjectionOnly);
	}

	/**
	 * Install the set of default selectors.
	 */
	private void initSelectors() {
		// Simple policy at the start. We can complicate later.
		fSelector = new RandomSelector(fView, fRandom);
		fFilter = ISelectionFilter.UP_FILTER;
	}

	public void reset() {
		this.init(fView.capacity(), fH, fS, fRandom, fDebug,
				fActiveInjectionOnly);
	}

	private void init(int cacheSize, int h, int s, Random r, boolean debug,
			boolean activeInjectionOnly) {
		if ((h + s) > (cacheSize / 2)) {
			throw new IllegalArgumentException(
					"The sum of healer and swapper parameters cannot "
							+ "exceed half of the cache size.");
		}

		fView = new View(cacheSize, r);
		fQueue = new QueueManager(fView);
		fH = h;
		fS = s;
		fDebug = debug;
		fRandom = r;
		fActiveInjectionOnly = activeInjectionOnly;

		// Selectors are hardwired for now, but could be made configurable
		// in the future.
		initSelectors();
	}

	// --------------------------------------------------------------------------
	// The peer sampling service.
	// --------------------------------------------------------------------------
	public Node selectPeer(Node source) {
		if (fQueue.runDelayedUpdates()) {
			fQueue.permute(fRandom);
		}

		Node node = fQueue.popFirst();

		if (node == null) {
			node = this.selectPeer();
		}

		return node;
	}
	
	public Node selectPeer(Node node, ISelectionFilter filter) {
		throw new UnsupportedOperationException(); 
	}
	
	public boolean supportsFiltering() {
		return false;
	}

	@Override
	public void clear(Node source) {
		// Nothing to clear.
	}

	// --------------------------------------------------------------------------
	// CDProtocol implementations (and helper methods)
	// --------------------------------------------------------------------------

	public void nextCycle(Node thisNode, int protocolID) {
		// Selects a live peer.
		Node peerNode = selectPeer();

		// No live peers are known, nothing to do.
		if (peerNode == null) {
			return;
		}

		NewscastSN peer = (NewscastSN) peerNode
				.getProtocol(protocolID);

		// Linkables representing the social networks.
		Linkable thisSn = (Linkable) thisNode.getProtocol(FastConfig
				.getLinkable(protocolID));
		Linkable peerSn = (Linkable) peerNode.getProtocol(FastConfig
				.getLinkable(protocolID));

		verify(peer, thisSn, peerSn, false);
		this.doCycle(peer, thisSn, peerSn, thisNode, peerNode);
		verify(peer, thisSn, peerSn, true);
	}

	// --------------------------------------------------------------------------

	private void verify(NewscastSN peer, Linkable thisSn,
			Linkable peerSn, boolean queue) {
		if (fDebug) {
			this.view().verifyFriends(thisSn);
			peer.view().verifyFriends(peerSn);

			if (queue) {
				fQueue.verify();
			}
		}
	}

	// --------------------------------------------------------------------

	private Node selectPeer() {
		// View is empty - nothing to select.
		if (fView.size() == 0) {
			return null;
		}

		Node peer = fSelector.selectPeer(fFilter);
		if (CommonState.getNode().getID() == 400) {
			if (peer == null) {
				System.err.println("NULL!");
			}
		}
		return peer;
	}

	// --------------------------------------------------------------------

	void doCycle(NewscastSN peer, Linkable thisSn, Linkable peerSn,
			Node thisNode, Node peerNode) {
		// Resets the storage buffers.
		View.beginExchange();

		// shuffles the views.
		peer.view().permute();
		this.view().permute();

		// move oldest H items to end of view
		this.view().oldestInTheEnd(fH);
		peer.view().oldestInTheEnd(fH);

		this.select(fH, fS, peer, peerSn, this, thisNode, View.RECEIVER, true);
		this.select(fH, fS, this, thisSn, peer, peerNode, View.SENDER, !fActiveInjectionOnly);

		peer.view().buffer(View.RECEIVER).store();
		this.view().buffer(View.SENDER).store();

		// Ages the items.
		this.view().increaseAge();
		peer.view().increaseAge();

		// Updates the underlying queue.
		this.updateQueue();
		peer.updateQueue();

		// Marks a change.
		viewChanged();
	}

	// --------------------------------------------------------------------

	/**
	 * Builds the receiver's new view into one of the available manipulation
	 * areas. Note that this method DOES NOT change the actual view. A call to
	 * {@link BufferHandler#store()} is needed to commit the changes.
	 */
	void select(int h, int s, NewscastSN receiver, Linkable receiverSn,
			NewscastSN sender, Node senderNode, int buffer, boolean addFresh) {

		// Copies the receiver's view into the manipulation area.
		receiver.view().buffer(buffer).load();

		/**
		 * Appends the sending node's view head in the manipulation area. The
		 * result is an array composed of two parts:
		 * 
		 * [c][c/2 - 1]
		 * 
		 * where the first part comes from the sender, and the rest from the
		 * receiver.
		 */
		sender.view().buffer(buffer).appendHead(addFresh ? senderNode : null, receiverSn);

		/**
		 * Removes H oldest elements, always from the manipulation area (healer
		 * step). Note that this method biases the cache, but that is okay since
		 * "permute" is going to re-shuffle it before the next exchange anyway
		 */
		receiver.view().buffer(buffer).removeOldest(fH);

		/**
		 * Removes S elements from the manipulation area. These S elements are
		 * part of the receiver's view that is to be sent back to the sender.
		 */
		receiver.view().buffer(buffer).removeHead(fS);

		// Randomly shrink the manipulation area until it fits in
		// planned size for the views.
		receiver.view().buffer(buffer).shrinkRandom();
	}

	// --------------------------------------------------------------------------
	// Linkable implementations
	// --------------------------------------------------------------------------

	public boolean addNeighbor(Node neighbor) {
		boolean appended = fView.append(neighbor, 0);

		if (appended) {
			fQueue.updateDelayed();
		}

		return appended;
	}

	// --------------------------------------------------------------------------

	public boolean contains(Node neighbor) {
		return fView.contains(neighbor);
	}

	// --------------------------------------------------------------------------

	/** Might be less than cache size. */
	public int degree() {
		return fView.size();
	}

	// --------------------------------------------------------------------------

	public Node getNeighbor(int i) {
		return fView.getNode(i);
	}

	// --------------------------------------------------------------------------

	public void pack() {
	}

	// --------------------------------------------------------------------------

	public void onKill() {
		fView = null;
	}

	// --------------------------------------------------------------------------
	// Misc methods (accessors and other helpers)
	// --------------------------------------------------------------------------

	View view() {
		return fView;
	}

	// --------------------------------------------------------------------------

	public Object clone() {
		NewscastSN clone;
		try {
			clone = (NewscastSN) super.clone();
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}

		clone.fView = new View(view());
		clone.fQueue = fQueue.cloneUsing(clone.fView);
		clone.initSelectors();

		return clone;
	}

	// --------------------------------------------------------------------------

	public boolean hasChanged(int time) {
		return time < fLastChange;
	}

	// --------------------------------------------------------------------------

	private void viewChanged() {
		try {
			fLastChange = CommonState.getIntTime();
		} catch (ExceptionInInitializerError ex) {
			// Prints but swallows.
			ex.printStackTrace();
		}
	}

	private void updateQueue() {
		// Updates and reshuffles the queue.
		fQueue.update();
		fQueue.permute(fRandom);
	}

	// --------------------------------------------------------------------------
}

class QueueManager implements IExchanger {

	/**
	 * Set for speeding up merges.
	 */
	private static LinkedHashMap<Node, Object> fQueueHelper = new LinkedHashMap<Node, Object>();

	/**
	 * Queue for returning samples to clients.
	 */
	private Node[] fQueue;

	/**
	 * Counter indicating the length of {@link NewscastSN#fQueue}.
	 */
	private int fQueueLength = 0;

	/**
	 * Index of the next element to be returned.
	 */
	private int fHead = 0;

	private boolean fDelayedUpdate;

	/**
	 * View from which elements are drawn.
	 */
	private IView fView;

	// --------------------------------------------------------------------------

	public QueueManager(IView view) {
		fView = view;
		fQueue = new Node[view.capacity()];
	}

	// --------------------------------------------------------------------------

	public Node popFirst() {
		if (fHead == fQueueLength) {
			return null;
		}

		return fQueue[fHead++];
	}

	// --------------------------------------------------------------------------

	public boolean runDelayedUpdates() {
		if (fDelayedUpdate) {
			this.update();
			fDelayedUpdate = false;
			return true;
		}

		return false;
	}

	// --------------------------------------------------------------------------

	public void updateDelayed() {
		fDelayedUpdate = true;
	}

	// --------------------------------------------------------------------------

	public void update() {
		int size = fView.size();
		fQueueHelper.clear();

		for (int i = 0; i < size; i++) {
			fQueueHelper.put(fView.getNode(i), null);
		}

		for (int i = 0; i < fQueueLength; i++) {
			if (!fQueueHelper.containsKey(fQueue[i])) {
				if (i < fHead) {
					fHead--;
				}
				fQueue[i] = null;
			} else {
				fQueueHelper.remove(fQueue[i]);
			}
		}

		fQueueLength = MiscUtils.compact(fQueue, this, fQueueLength);

		Iterator<Node> it = fQueueHelper.keySet().iterator();
		while (it.hasNext()) {
			fQueue[fQueueLength++] = it.next();
		}

		fHead = Math.min(fHead, fQueueLength);
	}

	// --------------------------------------------------------------------------

	public void permute(Random r) {
		if (fHead == fQueueLength) {
			return;
		}

		OrderingUtils.permute(fHead, fQueueLength, this, r);
	}

	// --------------------------------------------------------------------------

	public void exchange(int i, int j) {
		rangeCheck(i);
		rangeCheck(j);

		Node tmp = fQueue[i];
		fQueue[i] = fQueue[j];
		fQueue[j] = tmp;
	}

	// --------------------------------------------------------------------------

	private void rangeCheck(int idx) {
		if (idx < 0 || idx >= fQueueLength) {
			throw new ArrayIndexOutOfBoundsException(idx);
		}
	}

	// --------------------------------------------------------------------------

	public void verify() {
		// View and queue must be of the same size.
		if (fQueueLength != fView.size()) {
			throw new AssertionError();
		}

		// Plus, every element here must be on the view (and vice-versa).
		for (int i = 0; i < fQueueLength; i++) {
			if (!fView.contains(fQueue[i])) {
				throw new AssertionError();
			}
		}
	}

	// --------------------------------------------------------------------------

	public QueueManager cloneUsing(IView viewClone) {
		QueueManager clone = new QueueManager(viewClone);
		System.arraycopy(fQueue, 0, clone.fQueue, 0, fQueue.length);

		return clone;
	}
}

/**
 * This class represents a partial view in our gossiping protocol. It contains
 * many useful operations for manipulating node and timestamp buffers
 * efficiently.
 * 
 * @author giuliano
 */
@SuppressWarnings("unchecked")
class View implements IExchanger, IView {

	// --------------------------------------------------------------------------
	// Static Fields
	// --------------------------------------------------------------------------

	public static final int SENDER = 0;
	public static final int RECEIVER = 1;

	private static Buffer[] fBuffers;

	// --------------------------------------------------------------------------
	// Static methods
	// --------------------------------------------------------------------------

	public static void init(int size) {
		if (fBuffers != null && (fBuffers[0].size() / 2) == size) {
			return;
		}

		fBuffers = new Buffer[2];
		fBuffers[SENDER] = new Buffer(2 * size);
		fBuffers[RECEIVER] = new Buffer(2 * size);
	}

	public static void beginExchange() {
		fBuffers[SENDER].reset();
		fBuffers[RECEIVER].reset();
	}

	// =================== fields ==========================================
	// =====================================================================

	private Random fRandom;
	private Node[] fNodes;
	private int[] fTStamps;
	private BufferHandler[] fHandlers = new BufferHandler[2];

	public View(int size, Random random) {
		View.init(size);
		fRandom = random;
		fNodes = new Node[size];
		fTStamps = new int[size];
		fHandlers[SENDER] = new BufferHandler(fBuffers[SENDER]);
		fHandlers[RECEIVER] = new BufferHandler(fBuffers[RECEIVER]);
	}

	// --------------------------------------------------------------------------

	public View(View other) {
		this(other.fNodes.length, other.fRandom);
		// Copies the caches. Note that we don't care about the BufferHandlers
		// being recreated as these operate on shared state.
		System.arraycopy(other.fNodes, 0, fNodes, 0, fNodes.length);
		System.arraycopy(other.fTStamps, 0, fTStamps, 0, fTStamps.length);
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#append(peersim.core.Node, int)
	 */
	public boolean append(Node node, int timestamp) {
		if (this.contains(node)) {
			return false;
		}

		int nxt = size();
		fNodes[nxt] = node;
		fTStamps[nxt] = timestamp;

		return true;
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#getNode(int)
	 */
	public Node getNode(int index) {
		return fNodes[index];
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#getTimestamp(int)
	 */
	public int getTimestamp(int index) {
		return fTStamps[index];
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#set(int, peersim.core.Node, int)
	 */
	public void set(int index, Node node, int timestamp) {
		fNodes[index] = node;
		fTStamps[index] = timestamp;
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#contains(peersim.core.Node)
	 */
	public boolean contains(Node wanted) {
		for (Node node : fNodes) {
			if (node != null && node.equals(wanted)) {
				return true;
			}
		}

		return false;
	}

	// --------------------------------------------------------------------------

	public BufferHandler buffer(int index) {
		return fHandlers[index];
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#permute()
	 */
	public void permute() {
		OrderingUtils.permute(0, size(), this, fRandom);
	}

	// --------------------------------------------------------------------------

	public void oldestInTheEnd(int kth) {
		int size = size();
		if (size == 0) {
			return;
		}
		OrderingUtils.orderByKthLargest(kth, 0, size - 1, fTStamps, this,
				fRandom);
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#increaseAge()
	 */
	public void increaseAge() {
		int size = size();
		for (int i = 0; i < size; i++) {
			fTStamps[i]++;
		}
	}

	// --------------------------------------------------------------------------

	public int capacity() {
		return fNodes.length;
	}

	// --------------------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.unitn.disi.protocol.IView#size()
	 */
	public int size() {
		int len = fNodes.length - 1;
		while (len >= 0 && fNodes[len] == null)
			len--;
		return len + 1;
	}

	// --------------------------------------------------------------------------

	public void exchange(int i, int j) {
		exchange(fNodes, fTStamps, i, j);
	}

	// --------------------------------------------------------------------------

	private void exchange(Node[] nodes, int[] tStamps, int i, int j) {
		Node tmp = nodes[i];
		nodes[i] = nodes[j];
		nodes[j] = tmp;

		int tmpi = tStamps[i];
		tStamps[i] = tStamps[j];
		tStamps[j] = tmpi;
	}

	// --------------------------------------------------------------------------

	public void verifyFriends(Linkable sn) {
		for (Node node : fNodes) {
			if (node == null) {
				break;
			}

			if (!sn.contains(node)) {
				throw new IllegalStateException("Verification failed.");
			}
		}
	}

	// --------------------------------------------------------------------------

	/**
	 * BufferHandler provides operations for transferring data from an
	 * underlying buffer and the encompassing {@link View} object.
	 */
	class BufferHandler implements IExchanger {

		private Buffer fBuffer;

		public BufferHandler(Buffer buffer) {
			fBuffer = buffer;
		}

		// --------------------------------------------------------------------------

		/**
		 * Loads the contents of the encompassing view onto the shared storage
		 * buffer.
		 */
		public void load() {
			int size = View.this.size();
			System.arraycopy(fNodes, 0, fBuffer.fNodes, fBuffer.fEnd, size);
			System.arraycopy(fTStamps, 0, fBuffer.fTStamps, fBuffer.fEnd, size);
			fBuffer.fEnd += size;
		}

		// --------------------------------------------------------------------------

		/**
		 * Returns a node in the shared buffer.
		 * 
		 * @param i
		 *            the index of the node to be returned.
		 * 
		 * @return the node. Note that if <b>i >= {@link #size()}</b>, the
		 *         return value is undetermined.
		 * 
		 * @throws ArrayIndexOutOfBoundsException
		 *             may be thrown if i >= {@link #size()}.
		 */
		Node getNode(int i) {
			return fBuffer.fNodes[fBuffer.fStart + i];
		}

		// --------------------------------------------------------------------------

		int getTimestamp(int i) {
			return fBuffer.fTStamps[fBuffer.fStart + i];
		}

		// --------------------------------------------------------------------------

		/**
		 * Appends the first element, plus c/2 - 1 elements from the parent view
		 * into the storage buffer.
		 * 
		 * Repeated elements will be discarded based on their timestamps --
		 * older elements are either removed from the storage buffer, or not
		 * appended. Note that this method will not replace the old entries with
		 * new entries, but rather remove the old entries from their original
		 * positions and append the new ones after the current end of the
		 * storage buffer.
		 * 
		 * @param firstElement
		 *            the {@link Node} object to which the encompassing view
		 *            belongs to.
		 * 
		 * @param socialNeighborhood
		 *            the social neighborhood of the node.
		 * 
		 */
		public void appendHead(Node firstElement, Linkable socialNeighborhood) {
			fBuffer.fMap.clear();

			// Stores the elements into the map.
			int size = size();
			for (int i = 0; i < size; i++) {
				fBuffer.fMap.put(fBuffer.fNodes[i], i);
			}

			if (firstElement != null) {
				// Appends fresh descriptor, if not null.
				appendUnique(firstElement, 0);
			}

			// Number of descriptors we'd like to append.
			int intended = capacity() / 2 - 1;

			// Appends the buffer, filtering out repetitions.
			for (int i = 0; i < View.this.size() && intended > 0; i++, intended--) {
				// Only appends if nodes are "friends".
				if (socialNeighborhood.contains(fNodes[i])) {
					appendUnique(fNodes[i], fTStamps[i]);
				}
			}

			fBuffer.fEnd = MiscUtils
					.compact(fBuffer.fNodes, this, fBuffer.fEnd);
		}

		// --------------------------------------------------------------------------

		private void appendUnique(Node entry, int timeStamp) {
			if (fBuffer.fMap.containsKey(entry)) {
				int old = (Integer) fBuffer.fMap.get(entry);
				if (fBuffer.fTStamps[old] <= timeStamp) {
					return;
				}
				// null out the entry if it's older.
				fBuffer.fNodes[old] = null;
			}
			// Appends if either not repeated or newer
			// than what was on the buffer.
			append(entry, timeStamp);
		}

		// --------------------------------------------------------------------------

		public int size() {
			return fBuffer.fEnd - fBuffer.fStart;
		}

		// --------------------------------------------------------------------------

		/**
		 * Removes the <b>h</b> oldest entries from the buffer, as long as
		 * {@link BufferHandler#size()} - h does not become smaller than the
		 * targeted view size. Otherwise, removes only enough elements to make
		 * it the right size.
		 * 
		 * @param h
		 *            the number of elements to remove.
		 * 
		 * @return the timestamp of the last element removed.
		 */
		public int removeOldest(int h) {
			int toRemove = Math.min(h, removalThreshold());
			if (toRemove <= 0) {
				return -1;
			}
			int removed = OrderingUtils.orderByKthLargest(fBuffer.fEnd
					- toRemove, fBuffer.fStart, fBuffer.fEnd - 1,
					fBuffer.fTStamps, this, fRandom);

			// FIXME should randomly rearrange the pivot (?)

			fBuffer.fEnd -= toRemove;
			return removed;
		}

		// --------------------------------------------------------------------------

		/**
		 * Removes <b>s</b> elements pertaining to the head of the enclosing
		 * view from the storage buffer, as long as {@link BufferHandler#size()}
		 * - s remains larger than the targeted view size. Otherwise, removes
		 * only enough elements to make it the right size.
		 * 
		 * @param s
		 *            the number of elements to remove.
		 */
		public void removeHead(int s) {
			int toRemove = Math.min(s, removalThreshold());
			if (toRemove <= 0) {
				return;
			}

			for (int i = 0; i < toRemove; i++) {
				if (!remove(fNodes[i])) {
					throw new RuntimeException(
							"Internal error: attempt to remove non-existent element.");
				}
			}

			fBuffer.fEnd = MiscUtils
					.compact(fBuffer.fNodes, this, fBuffer.fEnd);
		}

		// --------------------------------------------------------------------------

		private boolean remove(Node node) {
			for (int i = fBuffer.fStart; i < fBuffer.fEnd; i++) {
				if (node.equals(fBuffer.fNodes[i])) {
					if (fBuffer.fNodes[i] == null) {
						throw new RuntimeException(
								"Internal error: attempt to remove non-existent element.");
					}
					fBuffer.fNodes[i] = null;
					return true;
				}
			}

			return false;
		}

		// --------------------------------------------------------------------------

		public void shrinkRandom() {
			int toRemove = removalThreshold();
			for (int i = 0; i < toRemove; i++) {
				int idx = fBuffer.fStart + fRandom.nextInt(size() + 1);
				fBuffer.fNodes[idx] = null;
				fBuffer.fEnd = MiscUtils.compact(fBuffer.fNodes, this,
						fBuffer.fEnd);
			}
		}

		// --------------------------------------------------------------------------

		public void store() {
			int size = size();
			System.arraycopy(fBuffer.fNodes, fBuffer.fStart, fNodes, 0, size());
			System.arraycopy(fBuffer.fTStamps, fBuffer.fStart, fTStamps, 0,
					size());

			// Fill the rest of the fNodes array with nulls.
			for (int i = size; i < capacity(); i++) {
				fNodes[i] = null;
				fTStamps[i] = 0;
			}
		}

		// --------------------------------------------------------------------------

		private int removalThreshold() {
			return Math.max(0, fBuffer.fEnd - View.this.capacity());
		}

		// --------------------------------------------------------------------------

		private void append(Node node, int ts) {
			fBuffer.fNodes[fBuffer.fEnd] = node;
			fBuffer.fTStamps[fBuffer.fEnd] = ts;
			fBuffer.fMap.put(node, fBuffer.fEnd++);
		}

		// --------------------------------------------------------------------------

		public void exchange(int i, int j) {
			View.this.exchange(fBuffer.fNodes, fBuffer.fTStamps, i, j);
		}

		// --------------------------------------------------------------------------
	}

	/**
	 * Simple structure for holding a shared buffer.
	 */
	private static class Buffer {
		/** Node buffer. */
		Node[] fNodes;

		/** Timestamp buffer. */
		int[] fTStamps;

		/** Map for checking duplicates. */
		HashMap fMap;

		/** Position to be considered the start of the buffer. */
		int fStart;

		/** Position to be considered the end of the buffer. */
		int fEnd;

		/** The actual size of the buffers. */
		int fSize;

		Buffer(int size) {
			fNodes = new Node[size];
			fTStamps = new int[size];
			fMap = new HashMap();
			fSize = size;
		}

		// --------------------------------------------------------------------------

		int size() {
			return fSize;
		}

		// --------------------------------------------------------------------------

		void reset() {
			fStart = fEnd = 0;
		}

		// --------------------------------------------------------------------------
	}
}
