package it.unitn.disi.graph.analysis;

import java.util.ArrayList;

public interface ITopKEstimator {
	public ArrayList<? extends PathEntry> topKShortest(int source, int target, int k);
}
