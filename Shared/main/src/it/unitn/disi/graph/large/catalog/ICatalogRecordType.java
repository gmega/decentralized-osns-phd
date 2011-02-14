package it.unitn.disi.graph.large.catalog;

import java.util.List;

import it.unitn.disi.utils.logging.IBinaryRecordType;

/**
 * {@link ICatalogRecordType}s are like {@link IBinaryRecordType}s, except that
 * they have named parts.
 * 
 * @author giuliano
 */
public interface ICatalogRecordType extends IBinaryRecordType {

	public List<ICatalogPart<? extends Number>> getParts();
	
	/**
	 * @param key
	 *            a component identifier equal to one returned by
	 *            {@link #componentIndentifiers()}.
	 * 
	 * @return an index such that
	 *         <code>getParts().get(index).key().equals(key)</code>
	 *         returns <code>true</code>, or -1 if such index does not exist.
	 */
	public int indexOf(String key);
}
