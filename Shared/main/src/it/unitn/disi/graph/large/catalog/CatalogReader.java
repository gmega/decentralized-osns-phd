package it.unitn.disi.graph.large.catalog;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.EventCodec.DecodingStream;

/**
 * Provides an {@link Iterator} interface for reading {@link CatalogRecord}s
 * from an {@link InputStream}.
 * 
 * @author giuliano
 */
public class CatalogReader implements Iterator<CatalogRecord> {

	private EventCodec fDecoder = new EventCodec(Byte.class,
			CatalogRecordTypes.values());

	private DecodingStream fStream;

	private ICatalogRecordType fType;

	public CatalogReader(InputStream is, ICatalogRecordType type) {
		fStream = fDecoder.decodingStream(is);
	}

	@Override
	public boolean hasNext() {
		return fStream.hasNext();
	}

	@Override
	public CatalogRecord next() {
		List<Class<? extends Number>> parts = fType.components();
		Object[] record = new Object[parts.size()];
		for (int i = 0; i < parts.size(); i++) {
			Class<? extends Number> type = parts.get(i);
			record[i] = fStream.next(type);
		}
		return new CatalogRecord(fType, record);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
