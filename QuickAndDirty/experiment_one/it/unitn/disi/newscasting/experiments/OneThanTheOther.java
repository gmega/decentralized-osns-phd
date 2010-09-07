package it.unitn.disi.newscasting.experiments;

import java.nio.channels.IllegalSelectorException;

import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IEventObserver;


public class OneThanTheOther implements IPeerSelector, IEventObserver {
	
	private static final String PAR_N0 = "n0";
	
	private static Tweet fTweet;
	
	private static int [] fCounters;
	
	private int fNZero;
	
	private IPeerSelector fFirst;
	
	private IPeerSelector fSecond;
	
	public OneThanTheOther(IPeerSelector first, IPeerSelector second, String prefix) {
		fFirst = first;
		fSecond = second;
		fNZero = Configuration.getInt(prefix + "." + PAR_N0);
		
		if (fCounters == null) {
			fCounters = new int[Network.size()];
		}
	}

	@Override
	public Node selectPeer(Node source) {
		return selectPeer(source, ISelectionFilter.ALWAYS_TRUE_FILTER);
	}

	@Override
	public Node selectPeer(Node source, ISelectionFilter filter) {
		if (fTweet == null) {
			return null;
		}
		
		return fCounters[(int) source.getID()] < fNZero ? fFirst.selectPeer(
				source, filter) : fSecond.selectPeer(source, filter);
	}

	@Override
	public boolean supportsFiltering() {
		return true;
	}

	@Override
	public void clear(Node source) {
		fCounters[(int) source.getID()] = 0;
	}

	@Override
	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate) {
		if (fTweet != null && tweet != fTweet) {
			throw new IllegalSelectorException();
		}
		
		fCounters[(int) receiver.getID()] = Math.max(fCounters[(int) sender
				.getID()], fCounters[(int) receiver.getID()]);
	}

	@Override
	public void tweeted(Tweet tweet) {
		fTweet = tweet;
		fCounters[(int) tweet.poster.getID()] = 0;
	}
}
