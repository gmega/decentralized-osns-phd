package it.unitn.disi.graph.large.catalog;

import java.util.NoSuchElementException;

/**
 * Memory efficient interface for iterating catalogs.
 * 
 * @author giuliano
 */
public interface ICatalogCursor {

	/**
	 * Returns <tt>true</tt> if the iteration has more elements. (In other
	 * words, returns <tt>true</tt> if <tt>next</tt> would return an element
	 * rather than throwing an exception.)
	 * 
	 * @return <tt>true</tt> if the iterator has more elements.
	 */
	public boolean hasNext();

	/**
	 * Moves the cursor to the next element in the iteration.
	 * 
	 * @return the next element in the iteration.
	 * @exception NoSuchElementException
	 *                iteration has no more elements.
	 */
	public void next();

	/**
	 * Returns a field from the record over which the cursor is positioned.
	 * 
	 * @param key
	 *            the key of the field.
	 * @return the value of the field.
	 * @throws IllegalArgumentException
	 *             if no field with such key exists.
	 * 
	 * @throws IllegalStateException
	 *             if {@link #get(String)} is called before the first call to
	 *             {@link #next()}.
	 */
	public Number get(String key);

	/**
	 * Same as {@link #get(int)}, except that fields are referred by their
	 * position instead of their key.
	 * 
	 * @throws IllegalArgumentException if the index is non-existent.
	 */
	public Number get(int index);
}
