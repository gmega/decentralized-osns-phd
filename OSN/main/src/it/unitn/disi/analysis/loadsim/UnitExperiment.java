package it.unitn.disi.analysis.loadsim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UnitExperiment {
	
	private Map<Integer, Integer> fIndexes = new HashMap<Integer, Integer>();
	
	private ArrayList<int []> fData = new ArrayList<int []>();
	
	private Set<Integer> fSeen = new HashSet<Integer>();
	
	private Set<Integer> fROKeys = Collections.unmodifiableSet(fIndexes.keySet());
	
	private int fIndexAssignment = 0;
	
	private final int fId;
	
	private final int fDegree;
	
	public UnitExperiment(int id, int degree) {
		fId = id;
		fDegree = degree;
		this.newRound();
	}
	
	public void addData(int nodeId, int sent, int received) {
		// Repeated neighbor means we're starting a new round.
		if (fSeen.contains(nodeId)) {
			this.newRound();
		}
		
		int j = this.indexOf(nodeId);
		int [] row = topRow();
		row[sentIndex(j)] += sent;
		row[receivedIndex(j)] += received;
		fSeen.add(nodeId);
	}

	public void done() {
		fSeen = null;
		ArrayList<int []> compact = new ArrayList<int []>(fData.size());
		compact.addAll(fData);
		fData = compact;
	}
	
	public int duration() {
		return fData.size();
	}
	
	public int messagesReceived(int nodeId, int round) {
		check(nodeId, round);
		return fData.get(round)[receivedIndex(indexOf(nodeId))];
	}
	
	public int messagesSent(int nodeId, int round) {
		check(nodeId, round);
		return fData.get(round)[sentIndex(indexOf(nodeId))];
	}
	
	public int id() {
		return fId;
	}
	
	public int degree() {
		return fDegree;
	}
	
	public Set<Integer> participants() {
		return fROKeys;
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
		int [] row = new int[(fDegree + 1)*3];
		Arrays.fill(row, 0);
		fData.add(row);
		fSeen.clear();
	}
	
	private int[] topRow() {
		return fData.get(fData.size() - 1);
	}

	private int sentIndex(int j) {
		return j*3 + 1;
	}
	
	private int receivedIndex(int j) {
		return j*3 + 2;
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
}
