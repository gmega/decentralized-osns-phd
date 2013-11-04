package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.EventCodec;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Graph indexer takes an {@link IndexedNeighborGraph} and computes its an
 * attribute catalog containing indexing information.
 * 
 * @author giuliano
 */
public class GraphIndexer {

	private final ICatalogRecordType fType;

	private final byte[] fByteBuffer;

	private final Number[] fValueBuffer;

	private long fOffset;

	public GraphIndexer(ICatalogRecordType type) {
		fType = type;
		fByteBuffer = new byte[type.eventSet().sizeof(type)];
		fValueBuffer = new Number[type.components().size() + 1];
	}

	public void indexGraph(IndexedNeighborGraph inputGraph, OutputStream index,
			OutputStream indexedGraph) throws IOException {
		CatalogComputer computer = new CatalogComputer(inputGraph, fType);
		EventCodec encoder = new EventCodec(Byte.class,
				CatalogRecordTypes.values());
		fOffset = 0;

		while (computer.hasNext()) {
			computer.next();
			writeCatalogAttributes(computer, index, encoder);
			fOffset += writeNeighborhood(inputGraph, computer, indexedGraph);
		}
	}

	private void writeCatalogAttributes(ICatalogCursor cursor,
			OutputStream index, EventCodec codec) throws IOException {
		List<ICatalogPart<? extends Number>> parts = fType.getParts();
		// Magic number is the first part of record.
		fValueBuffer[0] = fType.magicNumber();

		// Then the rest.
		for (int i = 0; i < parts.size(); i++) {
			ICatalogPart<? extends Number> part = parts.get(i);
			if (part.key().equals("offset")) {
				fValueBuffer[i + 1] = fOffset;
			} else {
				fValueBuffer[i + 1] = cursor.get(i);
			}
		}

		// Encodes to byte and writes to file.
		int length = codec.encodeEvent(fByteBuffer, 0, fValueBuffer);
		index.write(fByteBuffer, 0, length);
	}

	private int writeNeighborhood(IndexedNeighborGraph graph,
			CatalogComputer computer, OutputStream output) throws IOException {
		int root = computer.currentNeighborhood();
		int degree = graph.degree(root);
		int written = 0;
		for (int i = 0; i < degree; i++) {
			written += writeInt(root, output);
			written += writeInt(graph.getNeighbor(root, i), output);
		}

		return degree * 2 * Integer.SIZE;
	}

	private int writeInt(int number, OutputStream ostream) throws IOException {
		int len = CodecUtils.append(number, fByteBuffer, 0);
		ostream.write(fByteBuffer, 0, len);
		return len;
	}

}
