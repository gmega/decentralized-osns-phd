package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.epidemics.IEventObserver;
import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IPeerSelector;
import it.unitn.disi.epidemics.ISelectionFilter;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.peersim.SNNode;

import java.nio.channels.IllegalSelectorException;

import peersim.config.IResolver;
import peersim.core.Network;
import peersim.core.Node;

public class OneThanTheOther implements IPeerSelector, IEventObserver {

	/**
	 * The switching parameter.
	 * 
	 * @config
	 */
	private static final String PAR_N0 = "n0";

	/**
	 * The single {@link Tweet} that's being propagated.
	 */
	private static IGossipMessage fTweet;

	/**
	 * Shared counters to facilitate programming.
	 */
	private static int[] fCounters;

	private int fNZero;

	private IPeerSelector fFirst;

	private IPeerSelector fSecond;

	public OneThanTheOther(IPeerSelector first, IPeerSelector second,
			IResolver resolver, String prefix) {
		this(first, second, resolver.getInt(prefix, PAR_N0));
	}

	public OneThanTheOther(IPeerSelector first, IPeerSelector second, int nzero) {
		fFirst = first;
		fSecond = second;
		fNZero = nzero;

		if (fCounters == null) {
			fCounters = new int[Network.size()];
		}
	}

	@Override
	public Node selectPeer(Node source, ISelectionFilter filter) {
		int idx = (int) source.getID();

		Node selected = null;
		if (fCounters[idx] < fNZero) {
			fCounters[idx]++;
			selected = fFirst.selectPeer(source, filter);
		}

		if (selected == null) {
			selected = fSecond.selectPeer(source, filter);
		}

		return selected;
	}

	@Override
	public void clear(Node source) {
		fCounters[(int) source.getID()] = 0;
	}

	@Override
	public void delivered(SNNode sender, SNNode receiver, IGossipMessage tweet,
			boolean duplicate) {
		if (fTweet != null && tweet != fTweet) {
			throw new IllegalSelectorException();
		}

		fCounters[(int) receiver.getID()] = Math.max(
				fCounters[(int) sender.getID()],
				fCounters[(int) receiver.getID()]);
	}

	@Override
	public void localDelivered(IGossipMessage tweet) {
		fTweet = tweet;
		fCounters[(int) tweet.originator().getID()] = 0;
	}

	public IPeerSelector first() {
		return fFirst;
	}

	public IPeerSelector second() {
		return fSecond;
	}

	public void setN0(int nZero) {
		this.fNZero = nZero;
	}
}
