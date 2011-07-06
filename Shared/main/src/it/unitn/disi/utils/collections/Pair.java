package it.unitn.disi.utils.collections;

/**
 * Generic representation for a pair.
 * 
 * @author giuliano
 *
 * @param <K1>
 * @param <K2>
 */
public class Pair <K1, K2>{
	public final K1 a;
	public final K2 b;
	
	private final int fHashCode;
	
	public Pair(K1 a, K2 b) {
		this.a = a;
		this.b = b;
		
		int hashCode = 957;
		hashCode += 37*((a == null) ? 0 : a.hashCode());
		hashCode += 37*((b == null) ? 0 : b.hashCode());
		fHashCode = hashCode;
	}
	
	@SuppressWarnings("rawtypes")
	public boolean equals(Object otherObj) {
		if (!(otherObj instanceof Pair)) {
			return false;
		}
		
		Pair other = (Pair) otherObj;
		boolean equals = (a == null) ? other.a == null : a.equals(other.a);
		equals &= (b == null) ? other.b == null : b.equals(other.b);
		return equals;
	}
	
	@Override
	public int hashCode() {
		return fHashCode;
	}
	
	@Override
	public String toString() {
		return "(" + a + ", " + b + ")";
	}
}
