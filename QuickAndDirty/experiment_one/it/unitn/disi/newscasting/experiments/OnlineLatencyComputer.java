package it.unitn.disi.newscasting.experiments;

import java.util.HashMap;

import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IEventObserver;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class OnlineLatencyComputer implements IEventObserver {
	
	private static final OnlineLatencyComputer fInstance = new OnlineLatencyComputer();
	
	public static OnlineLatencyComputer getInstance() {
		return fInstance;
	}
	
	private TweetData data;
	
	public void done() {
		if (data == null) {
			return;
		}
		
		StringBuffer stats = new StringBuffer();
		stats.append("DE:");
		stats.append(data.fTweet.profile().getID());
		stats.append(" ");
		stats.append(data.fTweet.destinations());
		stats.append(" ");
		stats.append(data.getMax());
		stats.append(" ");
		stats.append(data.getAvg());
		stats.append(" ");
		stats.append(data.getVar());
		stats.append(" ");
		stats.append(data.avgGain());
		stats.append(" ");
		stats.append(data.maxGain());
		stats.append(" ");
		stats.append(data.getDuplicates());
		System.out.println(stats);
		data = null;
	}

	@Override
	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate) {
		
		if (duplicate) {
			data.duplicate();
		} else {
			data.received();
		}
	}

	@Override
	public void tweeted(Tweet tweet) {
		if (data != null) {
			throw new IllegalArgumentException();
		}
		data = new TweetData(tweet);
	}

}

class TweetData {
	
	public Tweet fTweet;
	
	private int fPending;
	private int fTime;
	private int fDups;
	
	private IncrementalStats fLatency;
	
	public TweetData(Tweet tweet) {
		fPending = tweet.destinations();
		fTime = CommonState.getIntTime();
		fLatency = new IncrementalStats();
		fTweet = tweet;
	}
	
	public boolean received() {
		fPending--;
		if (fPending < 0) {
			throw new IllegalStateException();
		}
		
		fLatency.add(CommonState.getIntTime() - fTime);
		return fPending == 0;
	}
	
	public void duplicate() {
		fDups++;
	}
	
	public int getMax() {
		return (int) fLatency.getMax();
	}
	
	public int getMin() {
		return (int) fLatency.getMin();
	}
	
	public double getAvg() {
		return fLatency.getAverage();
	}
	
	public double getVar() {
		return fLatency.getVar();
	}
	
	public double maxGain() {
		return (double) (fTweet.destinations()) / getMax();
	}
	
	public double avgGain() {
		double worstAverage = (double) (fTweet.destinations() + 1) / 2.0;
		return worstAverage/getAvg();
	}
	
	public int getDuplicates(){ 
		return fDups;
	}
}
