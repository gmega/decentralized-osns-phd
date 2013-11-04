package it.unitn.disi.graph.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

@AutoConfig
public abstract class GraphAnalyzer implements ITransformer {
	
	private String fDecoder;
	
	public GraphAnalyzer(@Attribute("decoder") String decoder) {
		fDecoder = decoder;
	}

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		ResettableGraphDecoder decoder = GraphCodecHelper.createDecoder(is, fDecoder);
		
		LightweightStaticGraph graph = LightweightStaticGraph.load(decoder);
		this.transform(graph, oup);
	}
	
	protected abstract void transform(LightweightStaticGraph graph, OutputStream stream) throws IOException;	
}
