package it.unitn.disi.newscasting.experiments;


import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IEventObserver;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class OnlineLatencyComputer implements IEventObserver {
	
	private static final OnlineLatencyComputer fInstance = new OnlineLatencyComputer();
	
	public static OnlineLatencyComputer getInstance() {
		return fInstance;
	}
	
	private TweetData data;
	
	private int [] fSent;
	
	private int [] fReceived;
	
	private int [] fDups;
	
	public OnlineLatencyComputer() {
		fSent = new int[Network.size()];
		fReceived = new int[Network.size()];
		fDups = new int[Network.size()];
	}
	
	public void done() {
		if (data == null) {
			return;
		}
		StringBuffer stats = new StringBuffer();
		stats.append("DE:");
		stats.append(" ");
		stats.append(data.fTweet.profile().getID()); 	// 1(2) - id
		stats.append(" ");
		stats.append(data.fTweet.destinations());		// 2(3) - degree
		stats.append(" ");
		stats.append(data.getMax());					// 3(4) - max
		stats.append(" ");
		stats.append(data.getAvg());					// 4(5) - avg
		stats.append(" ");
		stats.append(data.getVar());					// 5(6) - var
		stats.append(" ");
		stats.append(data.avgGain());
		stats.append(" ");
		stats.append(data.maxGain());
		stats.append(" ");
		stats.append(data.getDuplicates());
		stats.append(" ");
		stats.append(data.getUndelivered());
		System.out.println(stats);
		data = null;
	}
	
	public void dumpStatistics(boolean [] nodes) {
		StringBuffer stats = new StringBuffer();
		stats.append("BEGIN_STATS_DUMP\n");
		for (int i = 0; i < fSent.length; i++) {
			if (nodes[i]) {
				stats.append("N:");
				stats.append(" ");
				stats.append(i);
				stats.append(" ");
				stats.append(fSent[i]);
				stats.append(" ");
				stats.append(fReceived[i]);
				stats.append(" ");
				stats.append(fDups[i]);
				stats.append("\n");
			}
		}
		stats.append("END_STATS_DUMP\n");
		System.out.println(stats.toString());
	}

	@Override
	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate) {
		
		if (duplicate) {
			data.duplicate();
			fDups[(int)receiver.getID()]++;
		} else {
			data.received();
			fReceived[(int)receiver.getID()]++;
		}
		
		fSent[(int)sender.getID()]++;
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
	
	public int getUndelivered() {
		return fPending;
	}
}
