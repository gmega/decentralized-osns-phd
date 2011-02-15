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
		
	}
}
