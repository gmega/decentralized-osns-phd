package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/** 
 * Command Line Interface frontend for {@link GraphIndexer}.
 * 
 * @author giuliano
 */
@AutoConfig
public class GraphIndexerCLI implements IMultiTransformer {

	public static enum Inputs {
		graph
	}

	public static enum Outputs {
		reordered_graph, catalog
	}

	@Attribute("decoder")
	private String fDecoder;

	@Override
	public void execute(StreamProvider provider) throws Exception {
		
		// Loads graph into memory (unfortunately no way around this). 
		ResettableGraphDecoder decoder = GraphCodecHelper.createDecoder(
				provider.input(Inputs.graph), fDecoder);
		LightweightStaticGraph graph = LightweightStaticGraph.load(decoder);
		
		OutputStream reordered = provider.output(Outputs.reordered_graph);
		OutputStream catalog = provider.output(Outputs.catalog);
		
		GraphIndexer idx = new GraphIndexer(CatalogRecordTypes.PROPERTY_RECORD);
		idx.indexGraph(graph, catalog, reordered);
		
		reordered.close();
		catalog.close();
	}

}
