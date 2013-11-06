package it.unitn.disi.utils.collections;

/**
 * An {@link IExchanger} knows how to exchange elements. The exact details of
 * what it exchanges are implementation-specific, but the original idea is
 * allowing clients to provide plugable exchange policies in sorting algorithms.
 * 
 * @author giuliano
 */
public interface IExchanger {
	/**
	 * Exchanges two elements, which are identified by numeric IDs.
	 */
	public void exchange(int i, int j);
}

