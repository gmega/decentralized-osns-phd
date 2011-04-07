package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.peersim.SNNode;

import java.io.PrintStream;

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
public class ExperimentStatisticsManager implements IEventObserver,
		IExperimentObserver {

	private static final ExperimentStatisticsManager fInstance = new ExperimentStatisticsManager();

	public static ExperimentStatisticsManager getInstance() {
		return fInstance;
	}

	private UnitExperimentData fCurrentExperiment;

	/**
	 * Headers must be printed once. These flags are a cheap way to implement
	 * that.
	 */
	private boolean fPrintedLatencyHeader;
	private boolean fPrintedLoadHeader;

	private ExperimentStatisticsManager() {
	}

	public void printLatencyStatistics(PrintStream stream) {
		if (fCurrentExperiment != null) {
			printLatencyHeader();
			System.out.println(fCurrentExperiment.latencyStatistics());
			System.out.println("RT: "
					+ fCurrentExperiment.fTweet.poster.getID() + " "
					+ fCurrentExperiment.disseminationTime());
		}
	}

	public void printLoadStatistics(PrintStream stream) {
		if (fCurrentExperiment != null) {
			printLoadHeader();
			System.out.println(fCurrentExperiment.loadStatistics());
		}
	}

	private void printLatencyHeader() {
		if (!fPrintedLatencyHeader) {
			System.out.println(fCurrentExperiment.latencyFields());
			fPrintedLatencyHeader = true;
		}
	}

	private void printLoadHeader() {
		if (!fPrintedLoadHeader) {
			System.out.println(fCurrentExperiment.loadFields());
			fPrintedLoadHeader = true;
		}
	}

	@Override
	public void eventDelivered(SNNode sender, SNNode receiver, Tweet tweet,
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

	@Override
	public void experimentEnd(Node root) {
		if (fCurrentExperiment == null) {
			return;
		}

		fCurrentExperiment = null;
	}

	@Override
	public void experimentStart(Node root) {
	}

	@Override
	public void experimentCycled(Node root) {
	}
}

/**
 * Stores data regarding an on-going unit experiment.
 * 
 * @author giuliano
 */
class UnitExperimentData {

	/**
	 * All lines related to load data are prefixed by this.
	 */
	private static final String LOAD_PREFIX = "N:";

	/**
	 * All lines related to latency data are prefixed by this.
	 */
	private static final String LATENCY_PREFIX = "DE:";

	/**
	 * Character which separates fields for all data we print here.
	 */
	private static final String FIELD_SEPARATOR = " ";

	/**
	 * Number of messages received by each node (tracks mainly duplicates).
	 */
	private static final int[] fReceived = new int[Network.size()];

	/**
	 * Number of messages sent by each node.
	 */
	private static final int[] fSent = new int[Network.size()];

	/**
	 * Number of messages sent by each node which were duplicates.
	 */
	private static final int[] fDuplicatesSent = new int[Network.size()];

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
		fPending = tweet.destinations() - 1;
		fTime = CommonState.getIntTime();
		fTweet = tweet;
		clear();
	}

	private void clear() {
		for (int i = 0; i < fReceived.length; i++) {
			fReceived[i] = 0;
			fSent[i] = 0;
			fDuplicatesSent[i] = 0;
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
	public boolean messageDelivered(SNNode sender, SNNode receiver) {
		fPending--;
		if (fPending < 0) {
			throw new IllegalStateException();
		}

		fLatency.add(receiver.uptime());
		count(sender.getID(), receiver.getID(), false);
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
		count(sender.getID(), receiver.getID(), true);
	}

	private void count(long sender, long receiver, boolean dup) {
		fSent[(int) sender]++;
		fReceived[(int) receiver]++;
		if (dup) {
			fDuplicatesSent[(int) sender]++;
		}
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
	public int duplicates() {
		return received() - delivered();
	}

	/**
	 * @return a count of the messages delivered to all nodes during this unit
	 *         experiment.
	 */
	public int delivered() {
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
	 * @return the sum for all received message latencies.
	 */
	public double getSum() {
		return fLatency.getSum();
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
		return (double) (fTweet.destinations() - 1) / getMax();
	}

	/**
	 * @return the average speedup over the naïve dissemination approach.
	 */
	public double averageSpeedup() {
		return ((fTweet.destinations()) * delivered()) / (2.0 * getSum());
	}

	/**
	 * @return time it took for the message to reach all destinations.
	 */
	public int disseminationTime() {
		return CommonState.getIntTime() - fTime;
	}

	public String latencyFields() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(LATENCY_PREFIX);
		buffer.append("id");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("degree");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("t_max");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("latency_sum");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("t_var");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("avg_speedup");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("max_speedup");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("duplicates");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("undelivered");
		return buffer.toString();
	}

	public String latencyStatistics() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(LATENCY_PREFIX);
		buffer.append(fTweet.profile().getID()); // 1(2) - id
		buffer.append(FIELD_SEPARATOR);
		buffer.append(fTweet.destinations() - 1); // 2(3) - degree
		buffer.append(FIELD_SEPARATOR);
		buffer.append(getMax()); // 3(4) - max
		buffer.append(FIELD_SEPARATOR);
		buffer.append(getSum()); // 4(5) - latency sum
		buffer.append(FIELD_SEPARATOR);
		buffer.append(getVar()); // 5(6) - var
		buffer.append(FIELD_SEPARATOR);
		buffer.append(averageSpeedup());
		buffer.append(FIELD_SEPARATOR);
		buffer.append(minimumSpeedup());
		buffer.append(FIELD_SEPARATOR);
		buffer.append(duplicates());
		buffer.append(FIELD_SEPARATOR);
		buffer.append(undelivered());
		return buffer.toString();
	}

	public String loadFields() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(LOAD_PREFIX);
		buffer.append("root");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("id");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("sent");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("dups_sent");
		buffer.append(FIELD_SEPARATOR);
		buffer.append("received");
		// FIXME uptime not really a load statistic, but a per-node statistic
		// (as opposed to the latency aggregates, which are per-experiment
		// statistic). As long as we don't need more of those, I won't
		// generalize the implementation.
		buffer.append(FIELD_SEPARATOR);
		buffer.append("uptime");
		return buffer.toString();
	}

	public String loadStatistics() {
		StringBuffer buffer = new StringBuffer();
		// Appends the statistics.
		for (int i = 0; i < fTweet.destinations(); i++) {
			appendLoad(buffer, (SNNode) fTweet.destination(i));
			buffer.append("\n");
		}
		return buffer.toString();
	}

	private void appendLoad(StringBuffer buffer, SNNode node) {
		int i = (int) node.getID();
		buffer.append(LOAD_PREFIX);
		// 1 - Originator (identifies the unit experiment).
		buffer.append(fTweet.poster.getID());
		buffer.append(FIELD_SEPARATOR);
		// 2 - The node for which the statistics we are about to printed.
		buffer.append(i);
		buffer.append(FIELD_SEPARATOR);
		// 3 - Messages sent by the node.
		buffer.append(fSent[i]);
		buffer.append(FIELD_SEPARATOR);
		// 4 - Messages sent by this node which ended up being duplicates.
		buffer.append(fDuplicatesSent[i]);
		buffer.append(FIELD_SEPARATOR);
		// 5 - Messages received by the node. Note that this is COUNTING
		// DUPLICATES.
		buffer.append(fReceived[i]);
		buffer.append(FIELD_SEPARATOR);
		// 6 - Uptime.
		buffer.append(node.uptime());
	}
}
