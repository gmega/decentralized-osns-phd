package it.unitn.disi.graph.large.catalog;

/**
 * A record in the attribute catalog.
 * 
 * @author giuliano
 */
public class CatalogRecord {

	private final ICatalogRecordType fType;
	
	private final Object[] fRecords;

	CatalogRecord(ICatalogRecordType type, Object... records) {
		fRecords = records;
		fType = type;
	}

	/**
	 * @return the internal record array. Clients should not call this method.
	 */
	Object[] records() {
		return fRecords;
	}

	/**
	 * Returns an attribute by its key.
	 * 
	 * @param key
	 *            the key of an attribute.
	 * @return its value, or <code>null</code> if the attribute doesn't exist.
	 */
	public Object get(String key) {
		Integer index = fType.indexOf(key);
		if (index == null) {
			return null;
		}
		return get(index);
	}

	/**
	 * Returns an attribute by its index.
	 * 
	 * @param i
	 *            the index of the attribute.
	 * @return its value.
	 * 
	 * @throws ArrayIndexOutOfBoundsException
	 *             if i > {@link #size()}
	 */
	public Object get(int i) {
		return fRecords[i];
	}

	/**
	 * @return the length of this catalog record.
	 */
	public int size() {
		return fRecords.length;
	}

}
