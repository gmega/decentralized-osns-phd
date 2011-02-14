package it.unitn.disi.graph.large.catalog;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.EventCodec;

/**
 * Given a graph, processes it and creates its attribute catalog.
 * 
 * @author giuliano
 */
@AutoConfig
public class CreateCatalog implements IMultiTransformer {

	public enum Inputs {
		graph;
	}

	public enum Outputs {
		graph, catalog;
	}

	private String fDecoder;

	private EventCodec fCatalogWriter = new EventCodec(Byte.class,
			CatalogRecordTypes.values());

	public CreateCatalog(@Attribute("decoder") String decoder) {
		fDecoder = decoder;
	}

	@Override
	public void execute(StreamProvider p) throws Exception {
		// Loads the graph.
		IndexedNeighborGraph graph = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(p.input(Inputs.graph),
						fDecoder));

		OutputStream catalog = p.output(Outputs.catalog);
		OutputStream reorderedGraph = p.output(Outputs.graph);
		
		ICatalogRecordType recordType = CatalogRecordTypes.PROPERTY_RECORD;
		
		long offset = 0;
		byte [] byteBuffer = new byte[CatalogRecordTypes.set.sizeof(recordType)];
		Number [] valueBuffer = new Number[recordType.getParts().size() + 1];
		for (int i = 0; i < graph.size(); i++) {
			writeCatalogAttributes(graph, i, offset, catalog, byteBuffer, valueBuffer);
			offset += writeNeighborhood(graph, i, reorderedGraph, byteBuffer);
		}
	}

	private void writeCatalogAttributes(IndexedNeighborGraph graph, int root,
			long offset, OutputStream catalog, byte[] buf, Number[] record)
			throws IOException {
		List<ICatalogPart<? extends Number>> parts = CatalogRecordTypes.PROPERTY_RECORD.getParts();
		// Magic number is the first part of record.
		record[0] = CatalogRecordTypes.PROPERTY_RECORD.magicNumber();
		
		// Then the rest.
		for (int i = 0; i < parts.size(); i++) {
			ICatalogPart<? extends Number> part = parts.get(i);
			if (part.key().equals("offset")) {
				record[i + 1] = offset;
			} else {
				record[i + 1] = part.compute(graph, root);
			}
		}
		int length = fCatalogWriter.encodeEvent(buf, 0, record);
		catalog.write(buf, 0, length);
	}

	private int writeNeighborhood(IndexedNeighborGraph graph, int root,
			OutputStream stream, byte[] buf) throws IOException {
		int degree = graph.degree(root);
		for (int i = 0; i < degree; i++) {
			writeInt(root, buf, stream);
			writeInt(graph.getNeighbor(root, i), buf, stream);
		}
		return degree * 2 * Integer.SIZE;
	}
	
	private void writeInt(int number, byte[] buf, OutputStream ostream) throws IOException{
		int len = CodecUtils.append(number, buf, 0);
		ostream.write(buf, 0, len);
	}
}
