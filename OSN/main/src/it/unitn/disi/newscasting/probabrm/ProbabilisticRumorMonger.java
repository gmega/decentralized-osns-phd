package it.unitn.disi.newscasting.probabrm;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.PeersimUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;

public class ProbabilisticRumorMonger implements IContentExchangeStrategy, IEventObserver {

	/**
	 * ID of the protocol that uses this protocol. 
	 */
	private final int fParentProtocolId;
	
	/**
	 * ID of the (possibly dynamic) neighborhood {@link Linkable}.
	 */
	private final int fNeighborhoodId;
	
	/**
	 * ID of the (static or slowly changing) social network {@link Linkable}.
	 */
	private final int fSocialNetworkId;

	/**
	 * ID of the {@link ComponentComputationService} paired with the social
	 * network.
	 */
	private final int fCCSId;

	/**
	 * ID of the node that is coupled with this protocol. We use {@link Long}
	 * instead of long so that {@link NullPointerException} will be raised in
	 * case initialization is not performed properly.
	 */
	private Long fNodeId;
	
	/**
	 * If set to true, assumes that the {@link #fNeighborhoodId} points to a
	 * static neighborhood, and changes the sampling strategy to an eager 
	 * sampling strategy.
	 */
	private boolean fStatic;
	
	/**
	 * If set to true, overrides probability parameters with 1.0.
	 */
	private final boolean fFlood;
	
	/**
	 * Random number generator.
	 */
	private Random fRandom;

	/**
	 * Filter to be used by the selection heuristics.
	 */
	private final ISelectionFilter fFilter;

	/**
	 * Storage for the initial dissemination probabilities.
	 */
	private HashMap<Long, Double> fSendProbabilities = new HashMap<Long, Double>();

	/**
	 * Storage for the forwarding dissemination probabilities.
	 */
	private HashMap<Long, Double> fForwardProbabilities = new HashMap<Long, Double>();
	
	/**
	 * Queue containing pending operations.
	 */
	private LinkedList<ISend> fPendings = new LinkedList<ISend>();
	
	public ProbabilisticRumorMonger(int parentProtocolId, int snId, int ccsId,
			int neighborhoodId, ISelectionFilter filter, boolean flood,
			boolean statik, Random random) {
		fSocialNetworkId = snId;
		fCCSId = ccsId;
		fNeighborhoodId = neighborhoodId;
		fParentProtocolId = parentProtocolId;
		fFilter = filter;
		fFlood = flood;
		fStatic = statik;
		fRandom = random;
	}

	/**
	 * Called by the bootstrapping procedure, binds this instance to a node id.
	 * Not properly calling this method results in unspecified protocol
	 * behavior.
	 * 
	 * @param id
	 *            the id of the node associated to this instance.
	 */
	public void setNodeId(long id) {
		if (fNodeId != null && fNodeId != id) {
			throw new IllegalStateException("Cannot bind different IDs.");
		}
		fNodeId = id;
	}

	/**
	 * Sets with which probability this node should forward a message to a known
	 * neighbor of a node <b>B</b> when it receives a message from <b>B</b>.
	 * 
	 * @param id
	 *            the id of node <b>B</b>.
	 * 
	 * @param probability
	 *            the forwarding probability.
	 */
	public void setForwardProbability(Long id, Double probability) {
		fForwardProbabilities.put(id, probability);
	}
	
	/**
	 * Tells the probability with which this node should send messages to the
	 * component enclosing the receiving node, when this node is tweeting.
	 * 
	 * @param id
	 * @param probability
	 */
	public void setSendingProbability(Long id, Double probability) {
		fSendProbabilities.put(id, probability);
	}

	public boolean doExchange(Node source, Node peer) {
		Iterator<ISend> it = fPendings.iterator();
		ISend pending = null;
		Node actualPeer = null;
		while (actualPeer == null && it.hasNext()) {
			pending = it.next();
			actualPeer = pending.next(fFilter);
			if (pending.done()) {
				it.remove();
			}
		}

		// Nothing to do.
		if (actualPeer == null) {
			return false;
		}

		ICoreInterface peerAdaptable = (ICoreInterface) actualPeer
				.getProtocol(fParentProtocolId);
		peerAdaptable.receiveTweet(source, actualPeer, pending.getTweet(), this);
		return true;
	}
	
	public int throttling(Node node) {
		while (!fPendings.isEmpty()) {
			ISend op = fPendings.getFirst();
			if (!op.done()) {
				return op.getMinRounds();
			}
			
			fPendings.remove();
		}
		
		return 0;
	}
	
	public void tweeted(Node owner, int sequenceNumber) {
		// Queue one forwarding op per component.
		ComponentComputationService css = (ComponentComputationService) owner
				.getProtocol(fCCSId);
		Tweet msg = new Tweet(owner, sequenceNumber);
		for (int i = 0; i < css.components(owner); i++) {
			// The send probabilities are the same for connected components,
			// so we just pick the parameter computed for an arbitrary
			// member.
			List<Integer> members = css.members(owner, i);
			double probability = fFlood ? 1.0 : fSendProbabilities
					.get(members.get(0).longValue());
			fPendings.add(makeStrategy(owner, msg, fParentProtocolId,
					fNeighborhoodId, probability, members));
		}
	}

	public void eventDelivered(Node sender, Node receiver, Node owner,
			int start, int end) {

		// Sanity check.
		if (receiver.getID() != nodeId() || owner.getID() == nodeId()) {
			throw new IllegalStateException();
		}

		/** We are forwarding. **/
		Linkable ourNeighborhood = (Linkable) PeersimUtils.getLinkable(receiver,
				fParentProtocolId, fSocialNetworkId);
		Linkable senderNeighborhood = (Linkable) PeersimUtils.getLinkable(owner,
				fParentProtocolId, fSocialNetworkId);
		ArrayList<Object> intersection = new ArrayList<Object>();

		// Picks the intersection.
		double probability = fFlood ? 1.0 : fForwardProbabilities.get(owner
				.getID());
		MiscUtils.countIntersections(senderNeighborhood, ourNeighborhood,
				intersection, true);

		int i = (start == -1) ? end : start;
		for (; i <= end; i++) {
			Tweet msg = new Tweet(owner, i);
			fPendings.add(makeStrategy(receiver, msg, fParentProtocolId,
					fNeighborhoodId, probability, intersection));
		}
	}
	
	private DynamicDissemination makeStrategy(Node forwarder, Tweet tweet,
			int protocolId, int neighborhoodId, double probability,
			List<? extends Object> ids) {
		if (fStatic) {
			return eagerStrategy(forwarder, tweet, fParentProtocolId,
					fNeighborhoodId, probability, ids);
		} else {
			return dynamicStrategy(forwarder, tweet, fParentProtocolId,
					fNeighborhoodId, probability, ids);
		}
	}
	
	private DynamicDissemination eagerStrategy(Node forwarder, Tweet tweet,
			int protocolId, int neighborhoodId, double probability,
			List<? extends Object> ids) {
		ArrayList<Long> selected = new ArrayList<Long>();
		for (Object id : ids) {
			if (fRandom.nextDouble() < probability) {
				selected.add(((Number)id).longValue());
			}
		}
		// Eagerly picks the neighbors according to the probability parameter.
		DynamicDissemination dd = new DynamicDissemination(tweet, forwarder, protocolId, neighborhoodId, selected.size());
		for (Long id : selected) {
			dd.addNeighbor(id.longValue());
		}
		
		return dd;
	}
	
	private DynamicDissemination dynamicStrategy(Node forwarder, Tweet tweet,
			int protocolId, int neighborhoodId, double probability,
			List<? extends Object> ids) {
		int sampleSize = (int) Math.ceil(probability * ids.size());
		DynamicDissemination dd = new DynamicDissemination(tweet, forwarder,
				protocolId, neighborhoodId, sampleSize);
		
		for (Object id : ids) {
			dd.addNeighbor(((Number)id).longValue());
		}
		
		return dd;
	}
	
	public void duplicateReceived(Node sender, Node receiver, Node owner,
			int start, int end) {
		// XXX see if I should retransmit duplicates as well.
	}

	public int queueLength() {
		return fPendings.size();
	}
	
	public int pendingRounds(){
		int minRounds = 0;
		for (ISend send : fPendings) {
			minRounds += send.getMinRounds();
		}
		
		if (minRounds < 0 ) {
			throw new IllegalStateException();
		}
		
		return minRounds;
	}

	private Long nodeId() {
		if (fNodeId == null) {
			throw new IllegalStateException("Bad initialization.");
		}
		return fNodeId;
	}

	@Override
	public ActivityStatus status() {
		return ActivityStatus.PERPETUAL;
	}

	@Override
	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void tweeted(Tweet tweet) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear(Node source) {
		// TODO Auto-generated method stub
		
	}
}

/**
 * A pending send operation.
 * 
 * @author giuliano
 */
interface ISend {
	/**
	 * @return the next node to which a message should be sent. Returns null if
	 *         the node is down.
	 */
	public Node next(ISelectionFilter filter);
	
	/**
	 * @return the {@link Tweet} associated with this send operation.
	 */
	public Tweet getTweet();
	
	/**
	 * @return an estimate saying the minimal number of rounds this operation
	 *         will take to complete.
	 */
	public int getMinRounds();
	
	/**
	 * @return <code>true</code> if this send operation is done, or
	 *         <code>false</code> otherwise. When a send operation is node, all
	 *         subsequent calls to {@link #next()} will return <code>null</code>
	 *         .
	 */
	public boolean done();
}

/**
 * Dissemination strategy for nodes forwarding messages. Should work with
 * dynamic networks.
 * 
 * @author giuliano
 */
class DynamicDissemination implements ISend {
	
	// ----------------------------------------------------------------------
	private final int fProtocolId;
	
	private final int fSampledNeighborhood;
	// ----------------------------------------------------------------------

	/** Members of the static neighborhood over which our samples should come from. */
	private final Set<Long> fStaticNeighborhood = new HashSet<Long>();

	/** The node "forwarding" the message (sender). */
	private final Node fForwarder;
	
	/** The message being forwarded. */
	private final Tweet fTweet;
	
	/** Send cache. */
	private Node[] fToSend;
	
	/** Number of nodes we have in our sample. */
	private int fCurrentSize = 0;
	
	/** Number of nodes which we still need to contact. **/
	private int fRemaining;
	
	/** Timestamp of the last sample. **/
	private int fTimestamp = -1;
	
	// ----------------------------------------------------------------------

	public DynamicDissemination(Tweet tweet, Node forwarder, int protocolId,
			int neighborhoodId, int sampleSize) {
		fProtocolId = protocolId;
		fSampledNeighborhood = neighborhoodId;
		fForwarder = forwarder;
		fTweet = tweet;
		fRemaining = sampleSize;
		fToSend = new Node[sampleSize];
	}
	
	public void addNeighbor(long id) {
		fStaticNeighborhood.add(id);
	}
	
	public Node next(ISelectionFilter filter) {
		// Removes the garbage.
		// XXX This is an important thing to mention I'm doing.
		collectGarbage(fToSend);
		
		// Resamples to fill in the cache, if required.
		resample(fToSend, fProtocolId);

		// Picks a node.
		for (int i = fCurrentSize - 1; i >= 0; i--) {
			if (fToSend[i] != null) {
				Node toSend = fToSend[i];
				if (filter.canSelect(toSend)) {
					/** This operation works like a Taboo list. Previously
					 * sampled nodes won't be re-considered. */
					fStaticNeighborhood.remove(fToSend[i].getID());
					fToSend[i] = null;
					fRemaining--;
					fCurrentSize = MiscUtils.compact(fToSend, fCurrentSize);
					return filter.selected(toSend);	
				}
			}
		}

		return null;
	}
	
	private void collectGarbage(Node [] nodes) {
		// Expunge dead nodes.
		for (int i = 0; i < fCurrentSize; i++) {
			if (!nodes[i].isUp()) {
				nodes[i] = null;
			}
		}
		MiscUtils.compact(nodes, fCurrentSize);
	}
	
	private void resample(Node [] nodes, int protocolID) {
		Linkable neighborhood = (Linkable) PeersimUtils.getLinkable(fForwarder,
				protocolID, fSampledNeighborhood);
		
		if ((neighborhood instanceof IDynamicLinkable)) {
			if (!((IDynamicLinkable)neighborhood).hasChanged(fTimestamp)){
				return;
			}
		}
		
		// The neighborhood is assumed to be represent a non-biased sample.
		for (int i = 0; i < neighborhood.degree(); i++) {
			if (fCurrentSize == fRemaining) {
				break;
			}
			
			Node node = neighborhood.getNeighbor(i);
			if (fStaticNeighborhood.contains(node.getID())) {
				nodes[fCurrentSize++] = node;
			}
		}
		
		fTimestamp = CommonState.getIntTime();
	}

	public Tweet getTweet() {
		return fTweet;
	}
	
	public int getMinRounds() {
		if (fRemaining < 0) {
			throw new IllegalStateException();
		}
		
		return fRemaining;
	}

	public boolean done() {
		return fRemaining == 0;
	}
}
