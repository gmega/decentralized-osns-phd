package it.unitn.disi.utils.tabular;

/**
 * {@link ITableWriter} allows tabular data to be written in a simple way.
 * 
 * @author giuliano
 */
public interface ITableWriter {

	/**
	 * Sets a field in the current row to a value given as a {@link String}.
	 * Other <code>set</code> methods work similarly, but take other object
	 * types as values.
	 * 
	 * @param key
	 *            the key of the field to be set.
	 * 
	 * @param value
	 *            a {@link String} value.
	 * 
	 * @return <code>true</code> if the field key was valid, or
	 *         <code>false</code> otherwise.
	 */
	public abstract boolean set(String key, String value);

	/**
	 * Similar to {@link #set(String, String)}, but taking an <code>int</code>
	 * as value.
	 */
	public abstract boolean set(String key, int value);

	/**
	 * Similar to {@link #set(String, String)}, but taking an <code>long</code>
	 * as value.
	 */
	public abstract boolean set(String key, long value);

	/**
	 * Similar to {@link #set(String, String)}, but taking an
	 * <code>double</code> as value.
	 */
	public abstract boolean set(String key, double value);

	/**
	 * Similar to {@link #set(String, String)}, but taking an <code>float</code>
	 * as value.
	 */
	public abstract boolean set(String key, float value);

	/**
	 * Similar to {@link #set(String, String)}, but taking an
	 * <code>Object</code> as value.
	 */
	public abstract boolean set(String key, Object object);

	/**
	 * Starts a new row. All content input by <code>set</code> methods is
	 * discarded when this call is performed.
	 */
	public abstract void newRow();

	/**
	 * Emits the currently set row to an underlying output, and starts a new row
	 * by calling {@link #newRow()}.
	 * 
	 * @throws IllegalStateException
	 *             if there are fields which have not been set before this call
	 *             has been made.
	 */
	public abstract void emmitRow();

	/**
	 * @return an array containing the field keys in this table. All field keys
	 *         must be assigned values by calling the <code>set</code> methods
	 *         appropriately before {@link #emmitRow()} can be called.
	 */
	public abstract String[] fields();

}