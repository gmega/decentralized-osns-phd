package it.unitn.disi.utils;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * On several occasions, we want to map a given set of vertex IDs into a
 * continuous interval. This class facilitates the construction of such mapping
 * data structures, so that implementation can be faster and more efficient
 * mapping than plain {@link Map}s.
 * 
 * IDs are added to this "collection" through {@link #addMapping(int)}, and
 * assigned sequentially increasing numbers -- the "mapped IDs". Mapped IDs can
 * then be retrieved through {@link #map(int)}, or reversely mapped through
 * {@link #reverseMap(int)}.
 * 
 * @author giuliano
 */
public abstract class AbstractIDMapper implements IDMapper {

	public static final int UNMAPPED = Integer.MIN_VALUE;

	private int fAssignment;

	public AbstractIDMapper() {
	}

	/**
	 * Assigns a mapped ID to the current ID. Mapped IDs are drawn sequentially
	 * from an internal counter.
	 * 
	 * @return a newly-added mapped ID, or the old mapped ID if one already
	 *         existed.
	 */
	public int addMapping(int id) {
		if (id < 0) {
			throw new IllegalArgumentException("IDs can't be negative.");
		}
		int mapped = get(id);
		if (mapped == UNMAPPED) {
			addMapping(id, fAssignment);
			mapped = fAssignment;
			fAssignment++;
		}
		return mapped;
	}

	/**
	 * Returns the mapped ID.
	 * 
	 * @param id
	 *            the original ID.
	 * 
	 * @return the mapped ID.
	 */
	public int map(int id) {
		return map(id, false);
	}

	/**
	 * Returns the original ID.
	 * 
	 * @param id
	 *            a mapped ID.
	 * @return its original ID.
	 */
	public int reverseMap(int id) {
		return map(id, true);
	}

	private int map(int id, boolean reverse) {
		int mapped = reverse ? reverseGet(id) : get(id);
		if (mapped == UNMAPPED) {
			throw new NoSuchElementException("Unmapped id " + id + ".");
		}
		return mapped;
	}

	public boolean isMapped(int id) {
		return get(id) != UNMAPPED;
	}

	public int size() {
		return fAssignment;
	}

	public void clear() {
		fAssignment = 0;
	}

	protected abstract int get(int id);

	protected abstract int reverseGet(int id);

	protected abstract void addMapping(int id, int value);
}
