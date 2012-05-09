package it.unitn.disi.utils.collections;

public class Triplet<K1, K2, K3> {

	private static final long serialVersionUID = 1L;

	public final K1 a;
	public final K2 b;
	public final K3 c;

	private final int fHashCode;

	public Triplet(K1 a, K2 b, K3 c) {
		this.a = a;
		this.b = b;
		this.c = c;

		int hashCode = 957;
		hashCode += 37 * ((a == null) ? 0 : a.hashCode());
		hashCode += 37 * ((b == null) ? 0 : b.hashCode());
		hashCode += 37 * ((c == null) ? 0 : c.hashCode());
		fHashCode = hashCode;
	}

	@SuppressWarnings("rawtypes")
	public boolean equals(Object otherObj) {
		if (!(otherObj instanceof Triplet)) {
			return false;
		}

		Triplet other = (Triplet) otherObj;
		boolean equals = (a == null) ? other.a == null : a.equals(other.a);
		equals &= (b == null) ? other.b == null : b.equals(other.b);
		equals &= (c == null) ? other.c == null : c.equals(other.c);
		return equals;
	}

	@Override
	public int hashCode() {
		return fHashCode;
	}

	@Override
	public String toString() {
		return "(" + a + ", " + b + ", " + c + ")";
	}
}