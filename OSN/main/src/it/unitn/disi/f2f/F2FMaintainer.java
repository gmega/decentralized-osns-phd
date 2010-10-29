package it.unitn.disi.f2f;

import java.util.BitSet;

import it.unitn.disi.analysis.online.NodeStatistic;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.internal.selectors.IUtilityFunction;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.extras.am.epidemic.EpidemicProtocol;
import peersim.extras.am.epidemic.Message;

@AutoConfig
public class F2FMaintainer implements EpidemicProtocol, Linkable {

	// ----------------------------------------------------------------------
	// Constants and enums.
	// ----------------------------------------------------------------------

	static enum UtilityFunction {
		LOCAL, ORACLE
	}

	// ----------------------------------------------------------------------
	// Shared parameters and state.
	// ----------------------------------------------------------------------

	private static class Parameters {

		final int oneHop;

		final int threshold;

		final int statistics;

		final UtilityFunction utilityFunction;

		public Parameters(int oneHop, int threshold, int statistics,
				UtilityFunction utilityFunction, int maxMsgSize) {
			this.oneHop = oneHop;
			this.statistics = statistics;
			this.utilityFunction = utilityFunction;
			this.threshold = threshold;
		}
	}

	private static Parameters p;

	private static void oneShotInit(int oneHop, int threshold, int statistics,
			UtilityFunction function) {
		if (p != null) {
			return;
		}

		int maxSize = -1;
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			Linkable oneHopLinkable = (Linkable) node.getProtocol(oneHop);
			maxSize = Math.max(maxSize, oneHopLinkable.degree());
		}

		p = new Parameters(oneHop, threshold, statistics, function, maxSize);
	}

	// ----------------------------------------------------------------------
	// Protocol state.
	// ----------------------------------------------------------------------

	private IPeerSelector fSelector;

	private NeighborData[] fView;

	private NeighborData[] fNotInView;

	private Node fNode;

	public F2FMaintainer(@Attribute("one_hop") int oneHop,
			@Attribute("threshold") int threshold,
			@Attribute("utility_function") String utilityFunction,
			@Attribute("statistics_tracker") int statistics) {
		oneShotInit(oneHop, threshold, statistics,
				UtilityFunction.valueOf(utilityFunction.toUpperCase()));
		configure(p);
	}

	private void configure(Parameters p) {
	//	fSelector = peerSelector(p);
	}

	public void init(Node node) {
		Linkable neighborhood = (Linkable) node.getProtocol(p.oneHop);
		fNode = node;
		fView = new NeighborData[neighborhood.degree()];
		fNotInView = new NeighborData[neighborhood.degree()];

		for (int i = 0; i < fNotInView.length; i++) {
		//	fNotInView[i] = new NeighborData(i, p.threshold);
		}
	}

	// ----------------------------------------------------------------------
	// EpidemicProtocol interface.
	// ----------------------------------------------------------------------

	@Override
	public Node selectPeer(Node source) {
//		scrapePeerSampling(source);
		return fSelector.selectPeer(source);
	}

	@Override
	public Message prepareRequest(Node requester, Node receiver) {
		Digest message = new Digest();
		collectWanted(requester, statik(receiver), message.wanted);
		collectKnown(receiver, message.known);
		return message;
	}

	@Override
	public Message prepareResponse(Node responder, Node requester,
			Message request) {
		// Reuses the message object.
		Digest message = (Digest) request;
	//	unsetUnseen(message.getSender(), message.wanted);
		return message;
	}
	
	private void collectKnown(Node receiver, BitSet known) {
		for (NeighborData data : fView) {
			
		}
	}

	private int collectWanted(Node requester, Linkable receiver, BitSet wanted) {
		int count = 0;
		for (NeighborData data : fNotInView) {
			if (data.isElligible() && receiver.contains(data.node())) {
				if (wanted != null) {
					wanted.set(data.index());
				}
				count++;
			}
		}
		return count;
	}

	@Override
	public void merge(Node receiver, Node sender, Message msg) {
		Digest digest = (Digest) msg;
		BitSet sent = digest.known;
		
		// For each node that the sender knows ...
		int payloadSize = digest.known.size();
		Linkable senderSn = statik(sender);
		for (int i = sent.nextSetBit(0); i >= 0; i = sent.nextSetBit(i + 1)) {
			// ... sees if we needed it.
			Node received = senderSn.getNeighbor(i);
//			if (!addSeen(received)) {
//				// If we didn't, fakes that the sender didn't send it.
//				payloadSize--;
//			}
		}
		
		// Logs the digest exchange.
//		statistics(sender).messageReceived(size)

	}

	private NodeStatistic statistics(Node node) {
		return (NodeStatistic) node.getProtocol(p.statistics);
	}

	private Linkable statik(Node node) {
		return (Linkable) node.getProtocol(p.oneHop);
	}

	// ----------------------------------------------------------------------
	// Linkable interface.
	// ----------------------------------------------------------------------

	@Override
	public int degree() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Node getNeighbor(int i) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addNeighbor(Node neighbour) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean contains(Node neighbor) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void pack() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onKill() {
		// TODO Auto-generated method stub

	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		return null;
	}

}
