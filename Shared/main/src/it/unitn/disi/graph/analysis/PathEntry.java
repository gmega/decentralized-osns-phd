package it.unitn.disi.graph.analysis;

import java.util.Arrays;

public class PathEntry implements Comparable<PathEntry> {
	
	public int[] path;
	public final double cost;

	private final int hash;
	
	public PathEntry(int [] path, double cost) {
		this.path = path;
		this.cost = cost;
		this.hash = Arrays.hashCode(path);
	}
	
	@Override
	public int compareTo(PathEntry o) {
		return (int) Math.signum(this.cost - o.cost);
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof PathEntry)) {
			return false;
		}

		PathEntry oPath = (PathEntry) other;
		if (this.hash != oPath.hash) {
			return false;
		}

		return Arrays.equals(this.path, oPath.path);
	}

	@Override
	public String toString() {
		return Arrays.toString(path);
	}
	
}
