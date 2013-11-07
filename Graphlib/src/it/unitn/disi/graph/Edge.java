package it.unitn.disi.graph;

/**
 * 
 * @author giuliano
 */
public class Edge {
	
	public final int source;
	public final int target;
	public final boolean undirected;
	
	private volatile int fHashCode;
	
	public Edge(int source, int target, boolean undirected) {
		this.source = source;
		this.target = target;
		this.undirected = undirected;
	}
	
	@Override
	public int hashCode() {
		if (fHashCode == 0) {
			int result = 17;
			result = 37*result + (undirected ? 1 : 0);
			result = 37*result + (source + target);
			fHashCode = result;
		}
		
		return fHashCode;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Edge)) {
			return false;
		}
		
		Edge otherEdge = (Edge) other;
		boolean dirEquals = (source == otherEdge.source && target == otherEdge.target);
		
		if (undirected) {
			dirEquals |= (source == otherEdge.target && target == otherEdge.source);
		} 
		
		return dirEquals;
	}
	
	@Override
	public String toString() {
		return "(" + this.source + ", " + this.target + ")";
	}
}