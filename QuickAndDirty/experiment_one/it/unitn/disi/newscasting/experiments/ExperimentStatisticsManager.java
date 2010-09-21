package it.unitn.disi.newscasting.experiments;


import java.io.PrintStream;

import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IEventObserver;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

/**
 * {@link ExperimentStatisticsManager} tracks statistics for ongoing unit
 * experiments.
 * 
 * @author giuliano
 */
public class ExperimentStatisticsManager implements IEventObserver {
	
	private static final ExperimentStatisticsManager fInstance = new ExperimentStatisticsManager();
	
	public static ExperimentStatisticsManager getInstance() {
		return fInstance;
	}
	
	private UnitExperimentData fCurrentExperiment;
	
	private ExperimentStatisticsManager() { }
	
	public void done() {
		if (fCurrentExperiment == null) {
			return;
		}
		
		fCurrentExperiment = null;
	}
	
	public void printLatencyStatistics(PrintStream stream) {
		if (fCurrentExperiment != null) {
			System.out.println(fCurrentExperiment.latencyStatistics());
		}
	}
	
	public void printLoadStatistics(PrintStream stream) {
		if (fCurrentExperiment != null) {
			System.out.println(fCurrentExperiment.loadStatistics());
		}
	}
	
	@Override
	public void eventDelivered(Node sender, Node receiver, Tweet tweet,
			boolean duplicate) {
		if (duplicate) {
			fCurrentExperiment.duplicateReceived(sender, receiver);
		} else {
			fCurrentExperiment.messageDelivered(sender, receiver);
		}
	}

	@Override
	public void tweeted(Tweet tweet) {
		if (fCurrentExperiment != null) {
			throw new IllegalArgumentException();
		}
		fCurrentExperiment = new UnitExperimentData(tweet);
	}
}

/**
 * Stores data regarding an on-going unit experiment.
 * 
 * @author giuliano
 */
class UnitExperimentData {
	/**
	 * Number of messages received by each node (tracks mainly duplicates).
	 */
	private static final int [] fReceived = new int[Network.size()];
	
	/**
	 * Number of messages sent by each node.
	 */
	private static final int [] fSent = new int[Network.size()];
		
	/**
	 * {@link IncrementalStats} for latency statistics.
	 */
	private static final IncrementalStats fLatency = new IncrementalStats();
	
	/**
	 * The {@link Tweet} undergoing dissemination.
	 */
	public Tweet fTweet;
	
	/**
	 * The number of pending destinations.
	 */
	private int fPending;
	
	/**
	 * The time at which the current tweet has been produced.
	 */
	private int fTime;

	public UnitExperimentData(Tweet tweet) {
		fPending = tweet.destinations();
		fTime = CommonState.getIntTime();
		fTweet = tweet;
		clear();
	}
	
	private void clear() {
		for (int i = 0; i < fReceived.length; i++) {
			fReceived[i] = 0;
			fSent[i] = 0;
		}
		fLatency.reset();
	}

	/**
	 * Called when a message has been delivered, so that it can be accounted
	 * for.
	 * 
	 * @param sender
	 *            the node sending the message.
	 * 
	 * @param receiver
	 *            the node receiving the message.
	 * 
	 * @return <code>true</code> if the message has been <b>delivered</b> to all
	 *         of its destinations, or <code>false</code> otherwise.
	 */
	public boolean messageDelivered(Node sender, Node receiver) {
		fPending--;
		if (fPending < 0) {
			throw new IllegalStateException();
		}
		
		fLatency.add(CommonState.getIntTime() - fTime);
		count(sender.getID(), receiver.getID());
		return fPending == 0;
	}
	
	/**
	 * Called when a duplicate message has been received, so that it can be
	 * accounted for.
	 * 
	 * @param sender
	 *            the node sending the message.
	 * 
	 * @param receiver
	 *            the node receiving the message.
	 */
	public void duplicateReceived(Node sender, Node receiver) {
		count(sender.getID(), receiver.getID());
	}
	
	private void count(long sender, long receiver) {
		fSent[(int)sender]++;
		fReceived[(int)receiver]++;
	}
	
	/**
	 * @return received() + sent()
	 */
	public int totalMessages() {
		return received() + sent();
	}

	/**
	 * @return a count of the messages sent by all nodes during this unit
	 *         experiment.
	 */
	public int sent() {
		int sent = 0;
		for (int i = 0; i < fSent.length; i++) {
			sent += fSent[i];
		}
		
		return sent;
	}

	/**
	 * @return a count of the messages received by all nodes during this unit
	 *         experiment, including duplicates.
	 */
	public int received() {
		int received = 0;
		for (int i = 0; i < fReceived.length; i++) {
			received += fReceived[i];
		}
		return received;
	}

	/**
	 * @return a count of the duplicate messages received by all nodes during
	 *         this unit experiment.
	 */
	public int duplicates(){ 
		return received() - delivered() + undelivered();
	}

	/**
	 * @return a count of the messages delivered to all nodes during this unit
	 *         experiment.
	 */
	public int delivered () {
		return fTweet.destinations() - undelivered();
	}

	/**
	 * @return the number of destinations that still have not received the
	 *         message.
	 */
	public int undelivered() {
		return fPending;
	}
	
	/**
	 * @return the maximum message latency.
	 */
	public int getMax() {
		return (int) fLatency.getMax();
	}
	
	/**
	 * @return the minimum message latency.
	 */
	public int getMin() {
		return (int) fLatency.getMin();
	}
	
	/**
	 * @return the average message latency.
	 */
	public double getAvg() {
		return fLatency.getAverage();
	}
	
	/**
	 * @return the message latency variance.
	 */
	public double getVar() {
		return fLatency.getVar();
	}

	/**
	 * @return the minimum speedup over the naïve dissemination approach.
	 */
	public double minimumSpeedup() {
		return (double) (fTweet.destinations()) / getMax();
	}
	
	/**
	 * @return the average speedup over the naïve dissemination approach.
	 */
	public double averageSpeedup() {
		double worstAverage = (double) (fTweet.destinations() + 1) / 2.0;
		return worstAverage/getAvg();
	}
	
	public String latencyStatistics() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("DE:");
		buffer.append(" ");
		buffer.append(fTweet.profile().getID()); 	// 1(2) - id
		buffer.append(" ");
		buffer.append(fTweet.destinations());		// 2(3) - degree
		buffer.append(" ");
		buffer.append(getMax());					// 3(4) - max
		buffer.append(" ");
		buffer.append(getAvg());					// 4(5) - avg
		buffer.append(" ");
		buffer.append(getVar());					// 5(6) - var
		buffer.append(" ");
		buffer.append(averageSpeedup());
		buffer.append(" ");
		buffer.append(minimumSpeedup());
		buffer.append(" ");
		buffer.append(duplicates());
		buffer.append(" ");
		buffer.append(undelivered());
		return buffer.toString();
	}
	
	public String loadStatistics() {
		StringBuffer buffer = new StringBuffer();
		
		// Appends the statistics.
		appendLoad(buffer, fTweet.poster);
		buffer.append("\n");
		for (int i = 0; i < fTweet.destinations(); i++) {
			appendLoad(buffer, fTweet.destination(i));
			buffer.append("\n");
		}
		
		return buffer.toString();
	}

	private void appendLoad(StringBuffer buffer, Node node) {
		int i = (int) node.getID();
		buffer.append("N:");
		// 1 - Originator (identifies the unit experiment).
		buffer.append(fTweet.poster.getID());
		buffer.append(" ");
		// 2 - The node for which the statistics we are about to printed. 
		buffer.append(i);
		buffer.append(":");
		// 3 - Messages sent by the node.
		buffer.append(fSent[i]);
		buffer.append(":");
		// 3 - Messages received by the node.
		buffer.append(fReceived[i]);
		buffer.append(" ");
		// 4 - Duplicates: everything after the first message are duplicates.
		buffer.append(Math.max(0, fReceived[i] - 1));
	}
}
