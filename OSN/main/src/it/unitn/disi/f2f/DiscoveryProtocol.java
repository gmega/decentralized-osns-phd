package it.unitn.disi.f2f;

import java.util.BitSet;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.MulticastService;
import it.unitn.disi.epidemics.NeighborhoodMulticast;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.IInitializable;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
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
public class DiscoveryProtocol implements IEventObserver, IInitializable,
		CDProtocol {

	private static int fOneHop;

	private static int fTwoHop;

	private static int fMembership;

	private static int fMulticast;

	private static int fSelfPid;
	
	private static int fCollectPeriod;

	private static IMessageVisibility fVisibility;

	private BitSet fSeen = new BitSet();

	private int fSequence;

	private Node fNode;

	private IJoinListener fListener;
	
	private int fCycle;

	public DiscoveryProtocol(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("onehop") int oneHop, @Attribute("twohop") int twoHop,
			@Attribute("membership") int membership,
			@Attribute("multicast") int multicast,
			@Attribute("collect_period") int collect) {
		fOneHop = oneHop;
		fTwoHop = twoHop;
		fMembership = membership;
		fMulticast = multicast;
		fCollectPeriod = collect;
		fSelfPid = PeersimUtils.selfPid(prefix);
		fVisibility = new NeighborhoodMulticast(fTwoHop);
	}

	public void setJoinListener(IJoinListener listener) {
		fListener = listener;
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		if (fCycle++ % fCollectPeriod != 0) {
			return;
		}
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

	@Override
	public void delivered(SNNode sender, SNNode receiver,
			IGossipMessage message, boolean duplicate) {
		if (duplicate) {
			return;
		}
		AdvertisementMessage adv = (AdvertisementMessage) message;
		fromCurrentView(adv);
		fromPeerSampling(receiver, adv);
	}

	public void dropped(IGossipMessage msg) {
		AdvertisementMessage message = (AdvertisementMessage) msg;
		// If message has been forwarded means we're not the last ones
		// in the dissemination path.
		if (message.wasForwarded()) {
			return;
		}
		Node originator = message.originator();
		if (originator.isUp()) {
			DiscoveryProtocol protocol = (DiscoveryProtocol) originator
					.getProtocol(fSelfPid);
			protocol.home(fNode, (AdvertisementMessage) message);
		}
	}

	public void joinDone(IGossipMessage msg, JoinTracker tracker) {
		fListener.joinDone(msg, tracker.copies());
	}

	@Override
	public void localDelivered(IGossipMessage message) {
		// Makes no sense in our case.
	}

	/**
	 * A node is contacting ours with the results of our advertisement being
	 * disseminated.
	 * 
	 * @param sender
	 * @param message
	 */
	public void home(Node sender, AdvertisementMessage message) {
		message.or(fSeen);
	}

	public int size() {
		return fSeen.cardinality();
	}

	public double success() {
		return size()/((double) onehop().degree());
	}

	public boolean canContact(int idx) {
		return fSeen.get(idx);
	}

	@Override
	public void initialize(Node node) {
		fNode = node;
	}

	@Override
	public void reinitialize() {
		advertise();
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
	// Methods for collecting descriptors into passing messages.
	// ------------------------------------------------------------------------

	private void fromCurrentView(AdvertisementMessage adv) {
		Linkable lnk = onehop();
		for (int i = fSeen.nextSetBit(0); i != -1; i = fSeen.nextSetBit(i + 1)) {
			adv.add(lnk.getNeighbor(i));
		}
	}

	private void fromPeerSampling(Node ours, AdvertisementMessage adv) {
		Linkable membership = (Linkable) ours.getProtocol(fMembership);
		int members = membership.degree();
		for (int i = 0; i < members; i++) {
			Node member = (Node) membership.getNeighbor(i);
			adv.add(member);
		}
	}

	public Linkable onehop() {
		return (Linkable) fNode.getProtocol(fOneHop);
	}

	// ------------------------------------------------------------------------
	// Cloneable requirements.
	// ------------------------------------------------------------------------
	public Object clone() {
		try {
			DiscoveryProtocol theClone = (DiscoveryProtocol) super.clone();
			theClone.fSeen = new BitSet();
			theClone.fSeen.or(fSeen);
			return theClone;
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
}
