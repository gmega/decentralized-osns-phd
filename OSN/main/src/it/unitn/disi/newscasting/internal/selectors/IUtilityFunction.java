package it.unitn.disi.newscasting.internal.selectors;

/**
 * A {@link IUtilityFunction} can compute the utility of one object relative to
 * another.
 * 
 * @author giuliano
 */
public interface IUtilityFunction<K, V> {
	/**
	 * @return the relative utility of <code>target</code> with respect to
	 *         <code>base</code>. The value should be equal or larger than zero.
	 */
	public int utility(K base, V target);

	/**
	 * Tells whether or not the values for this utility function vary in time.
	 */
	public boolean isDynamic();
}
