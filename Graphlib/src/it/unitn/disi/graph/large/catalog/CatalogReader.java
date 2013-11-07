package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.large.catalog.CatalogReadsTest.CatalogRecord;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.EventCodec.DecodingStream;

import java.io.InputStream;
import java.util.List;

/**
 * Provides an {@link ICatalogCursor} interface for reading
 * {@link CatalogRecord}s from an {@link InputStream}.
 * 
 * @author giuliano
 */
public class CatalogReader extends AbstractCatalogCursor {

	private EventCodec fDecoder = new EventCodec(Byte.class,
			CatalogRecordTypes.values());

	private DecodingStream fStream;

	private boolean fStarted;

	public CatalogReader(InputStream is, ICatalogRecordType type) {
		super(type, false);
		fStream = fDecoder.decodingStream(is);
	}

	@Override
	public boolean hasNext() {
		return fStream.hasNext();
	}

	@Override
	public void next() {
		List<Class<? extends Number>> parts = type().components();
		Number [] buffer = valueBuffer();
		// Skips the magic number.
		fStream.next(Byte.class);
		for (int i = 0; i < parts.size(); i++) {
			Class<? extends Number> type = parts.get(i);
			buffer[i] = fStream.next(type);
		}
		fStarted = true;
	}

	@Override
	protected boolean isReady() {
		return fStarted;
	}
	
}
