package it.unitn.disi.network.churn;

@SuppressWarnings("rawtypes")
public class Delta<T extends Enum> {

	public final T current;

	public final T next;

	public Delta(T current, T next) {
		this.current = current;
		this.next = next;
	}
	
}
