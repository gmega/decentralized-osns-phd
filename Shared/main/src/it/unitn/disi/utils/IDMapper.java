package it.unitn.disi.utils;

import java.util.NoSuchElementException;

/**
 * Simple mapping interface which reassigns graph IDs.
 * 
 * @author giuliano
 */
public interface IDMapper {

	public final static IDMapper IDENTITY = new IDMapper() {
		@Override
		public int map(int i) {
			return i;
		}

		@Override
		public boolean isMapped(int i) {
			return true;
		}
	};

	/**
	 * @return the remapped ID.
	 * @throws NoSuchElementException
	 *             if element isn't mapped.
	 */
	public int map(int i);

	/**
	 * @return <code>true</code> if this element is mapped by this map, or
	 *         <code>false</code> otherwise.
	 */
	public boolean isMapped(int i);
}
