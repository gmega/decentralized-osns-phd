package it.unitn.disi.analysis.loadsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Stores and provides data for replaying unit experiment load data.
 * 
 * @author giuliano
 */
public class UnitExperiment {

	/**
	 * Allocation table for neighbors into the {@link #fData} arrays.
	 */
	private Map<Integer, Integer> fIndexes = new HashMap<Integer, Integer>();

	/**
	 * Array storing per-round messages sent and received.
	 */
	private ArrayList<int[]> fData = new ArrayList<int[]>();

	/**
	 * Nodes seen so far in the round being currently entered.
	 */
	private Set<Integer> fSeen = new HashSet<Integer>();

	/**
	 * Read-only view over the neighbor keys for returning to clients.
	 */
	private Set<Integer> fROKeys = Collections.unmodifiableSet(fIndexes
			.keySet());

	/**
	 * Cumulative sum so far (not used if
	 * {@link #UnitExperiment(int, int, boolean)} is called with cumulative =
	 * false).
	 */
	private int[] fCumSum;

	/**
	 * Counter for assigning indices to neighbors into the {@link #fData}
	 * arrays.
	 */
	private int fIndexAssignment = 0;

	/**
	 * ID of the root node for this {@link UnitExperiment}.
	 */
	private final int fId;

	/**
	 * Number of participants in the {@link UnitExperiment}.
	 */
	private final int fDegree;

	/**
	 * Number of infected participants.
	 */
	private final BitSet fInfected;

	/**
	 * When set to true, causes only the last round of information to be saved.
	 */
	private final boolean fLastOnly;

	/**
	 * Constructed a new unit experiment.
	 * 
	 * @param id
	 *            the node id at which the unit experiment is rooted (the
	 *            profile owner).
	 * @param degree
	 *            the size of the neighborhood.
	 * @param cumulative
	 *            whether the data passed to {@link #addData(int, int, int)}
	 *            represents cumulative figures or not.
	 * @param lastOnly
	 *            if <code>true</code>, saves only the last round of
	 *            information.
	 */
	public UnitExperiment(int id, int degree, boolean cumulative,
			boolean lastOnly) {
		fId = id;
		fDegree = degree;
		fInfected = new BitSet(degree);
		if (cumulative) {
			fCumSum = newRow();
		}
		fLastOnly = lastOnly;
		this.newRound();
	}

	/**
	 * Convenience constructor. Shorthand for:<BR>
	 * <code>UnitExperiment(id, degree, cumulative, false);</code>
	 */
	public UnitExperiment(int id, int degree, boolean cumulative) {
		this(id, degree, cumulative, false);
	}

	/**
	 * Adds new data to the unit experiment. Data should be entered per-round --
	 * i.e. only enter data for round X after you have finished entering the
	 * data for round X - 1. Further, the method should be called for <b>all
	 * nodes</b> in the first round, i.e., there should be exactly
	 * {@link UnitExperiment#degree()} calls in the first round.
	 * 
	 * @param nodeId
	 *            the node id for which data is being entered.
	 * @param sent
	 *            the number of messages sent in that round.
	 * @param received
	 *            the number of messages received in that round.
	 * 
	 * @throws IllegalStateException
	 *             if {@link #addData(int, int, int)} is called after
	 *             {@link #done()}.
	 */
	public void addData(int nodeId, int sent, int received) {
		if (isDone()) {
			throw new IllegalStateException(
					"Cannot add new data after call to done.");
		}
		// Repeated neighbor means we're starting a new round.
		if (fSeen.contains(nodeId)) {
			this.newRound();
		}

		int j = this.indexOf(nodeId);
		int[] row = topRow();

		int deltaSent = delta(sent, sentIndex(j));
		int deltaReceived = delta(received, receivedIndex(j));

		row[sentIndex(j)] += deltaSent;
		row[receivedIndex(j)] += deltaReceived;
		fSeen.add(nodeId);

		// Computes node infection.
		if (received > 0 && nodeId != fId) {
			fInfected.set(j);
		}
	}

	/**
	 * Called after all data for this unit experiment has been entered. Calls to
	 * {@link #addData(int, int, int)} are forbidden after a call to
	 * {@link #done()}, and might result in unspecified behavior.
	 */
	public void done() {
		fSeen = null;
		ArrayList<int[]> compact = new ArrayList<int[]>(fData.size());
		compact.addAll(fData);
		fData = compact;
	}

	/**
	 * @return whether {@link #isDone()} has been called or not.
	 */
	public boolean isDone() {
		return fSeen == null;
	}

	/**
	 * @return the duration (in rounds) of this unit experiment. Only returns
	 *         valid results after {@link #done()} has been called.
	 */
	public int duration() {
		return fData.size();
	}

	/**
	 * @return the number of messages received by a node (nodeId) at a given
	 *         round, in this unit experiment.
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if round >= {@link #duration()}.
	 */
	public int messagesReceived(int nodeId, int round) {
		check(nodeId, round);
		return fData.get(round)[receivedIndex(indexOf(nodeId))];
	}

	/**
	 * @return the number of messages sent by a node (nodeId) at a given round,
	 *         in this unit experiment
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if round >= {@link #duration()}.
	 */
	public int messagesSent(int nodeId, int round) {
		check(nodeId, round);
		return fData.get(round)[sentIndex(indexOf(nodeId))];
	}

	/**
	 * @return the id of the node that is the root of this
	 *         {@link UnitExperiment}.
	 */
	public int id() {
		return fId;
	}

	/**
	 * @return the number of nodes participating in this {@link UnitExperiment}.
	 */
	public int degree() {
		return fDegree;
	}

	/**
	 * @return a read-only set with the node IDs for the participants of this
	 *         {@link UnitExperiment}.
	 */
	public Set<Integer> participants() {
		return fROKeys;
	}

	/**
	 * @return the residue for this {@link UnitExperiment} (only makes sense
	 *         after the call to {@link #done()} has been performed).
	 */
	public double residue() {
		return undelivered() / ((double) fDegree);
	}

	/**
	 * @return the total number of undelivered message (only makes sense after
	 *         the call to {@link #done()} has been performed.
	 */
	public int undelivered() {
		return fDegree - fInfected.cardinality();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("unit experiment (id: ");
		buffer.append(fId);
		buffer.append(", ");
		buffer.append(fDegree);
		buffer.append(")");
		return buffer.toString();
	}

	// ----------------------------------------------------------------------
	// Private helpers.
	// ----------------------------------------------------------------------

	private int delta(int value, int index) {
		if (fCumSum == null) {
			return value;
		}

		int delta = value - fCumSum[index];
		if (delta < 0) {
			throw new IllegalStateException(
					"Cumulative values must be non-decreasing.");
		}
		fCumSum[index] = value;
		return delta;
	}

	private void check(int nodeId, int round) {
		if (round >= fData.size()) {
			throw new IllegalArgumentException("Round " + round
					+ " is invalid for " + this.toString() + ".");
		}

		if (!fIndexes.containsKey(nodeId)) {
			throw new IllegalArgumentException("Node " + nodeId
					+ " is invalid for " + this.toString() + ".");
		}
	}

	private void newRound() {
		boolean newRow = !fLastOnly || fData.size() == 0;
		int[] row = newRow ? newRow() : topRow();
		Arrays.fill(row, 0);
		if (newRow) {
			fData.add(row);
		}
		fSeen.clear();
	}

	private int[] newRow() {
		return new int[(fDegree + 1) * 2];
	}

	private int[] topRow() {
		return fData.get(fData.size() - 1);
	}

	private int sentIndex(int j) {
		return j * 2 + 0;
	}

	private int receivedIndex(int j) {
		return j * 2 + 1;
	}

	private int indexOf(int nodeId) {
		Integer index = fIndexes.get(nodeId);

		// Neighbor is unknown. Assigns it another index.
		if (index == null) {
			if (fIndexAssignment == (fDegree + 1)) {
				throw new IllegalArgumentException("Node " + fId
						+ ": too many nodes (" + (fDegree + 2) + ").");
			}
			index = fIndexAssignment++;
			fIndexes.put(nodeId, index);
		}

		return index;
	}
}
