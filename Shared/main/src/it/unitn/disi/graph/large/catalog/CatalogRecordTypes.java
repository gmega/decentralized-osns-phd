package it.unitn.disi.graph.large.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import it.unitn.disi.utils.logging.IBinaryRecordType;
import it.unitn.disi.utils.logging.RecordTypeSet;

/**
 * {@link CatalogRecordTypes} define the record structures that go into graph
 * catalogs.
 * 
 * @author giuliano
 */
public enum CatalogRecordTypes implements ICatalogRecordType {

	PROPERTY_RECORD(new NeighborhoodSize(),
			new NeighborhoodClustering(),
			new HollowPart<Long>(Long.class, "offset"));

	public static final RecordTypeSet<CatalogRecordTypes> set = new RecordTypeSet<CatalogRecordTypes>(
			CatalogRecordTypes.class);

	private final List<Class<? extends Number>> fComponents;

	private final List<ICatalogPart<? extends Number>> fKeys;
	
	private final HashMap <String,Integer> fIndexMap;

	private CatalogRecordTypes(ICatalogPart<? extends Number>... attributeTypes) {
		ArrayList<Class<? extends Number>> components = new ArrayList<Class<? extends Number>>();
		ArrayList<ICatalogPart<? extends Number>> keys = new ArrayList<ICatalogPart<? extends Number>>();
		fIndexMap = new HashMap<String, Integer>();
		
		for (int j = 0; j < attributeTypes.length; j++) {
			ICatalogPart<? extends Number> attributeType = attributeTypes[j];
			components.add((Class<? extends Number>) attributeType.returnType());
			keys.add(attributeType);
			fIndexMap.put(attributeType.key(), j);
		}
		
		fComponents = Collections.unmodifiableList(components);
		fKeys = Collections.unmodifiableList(keys);
	}

	@Override
	public Byte magicNumber() {
		return 0;
	}

	@Override
	public List<Class<? extends Number>> components() {
		return fComponents;
	}

	@Override
	public String formattingString() {
		return null;
	}

	@Override
	public RecordTypeSet<? extends Enum<? extends IBinaryRecordType>> eventSet() {
		return set;
	}

	@Override
	public List<ICatalogPart<? extends Number>> getParts() {
		return fKeys;
	}

	@Override
	public int indexOf(String key) {
		Integer index = fIndexMap.get(key);
		return index == null ? -1 : index;
	}

}
