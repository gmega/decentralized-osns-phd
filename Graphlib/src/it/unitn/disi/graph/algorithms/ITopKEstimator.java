package it.unitn.disi.graph.algorithms;

import java.util.ArrayList;

public interface ITopKEstimator {
	public ArrayList<? extends PathEntry> topKShortest(int source, int target, int k);
}
