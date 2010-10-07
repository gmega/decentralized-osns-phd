package it.unitn.disi.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.unitn.disi.codecs.GraphCodecHelper;
import it.unitn.disi.codecs.GraphEncoder;
import it.unitn.disi.utils.graph.LightweightStaticGraph;
import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class ComputeOrderGraph implements ITransformer {
	
	private final String fEncoderClass;
	
	private final String fDecoderClass;
	
	private final int fOrder;
	
	public ComputeOrderGraph(
			@Attribute("order") int order,
			@Attribute(value = "decoder", defaultValue = "it.unitn.disi.codecs.AdjListGraphDecoder") String decoder,
			@Attribute(value = "encoder", defaultValue = "it.unitn.disi.codecs.AdjListGraphEncoder") String encoder
			) {
		fEncoderClass = encoder;
		fDecoderClass = decoder;
		fOrder = order;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		LightweightStaticGraph graph = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(is, fDecoderClass));

		// Computes n-th order graph.
		graph = LightweightStaticGraph.transitiveGraph(graph, fOrder);
		
		// Prints it out.
		GraphEncoder encoder = GraphCodecHelper.createEncoder(oup, fEncoderClass);
		encoder.encode(graph);
	}
}
