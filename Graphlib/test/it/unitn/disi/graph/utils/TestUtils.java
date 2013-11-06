package it.unitn.disi.graph.utils;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.cli.Adj2ByteGraph;
import it.unitn.disi.graph.cli.Undirect;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.codecs.ByteGraphEncoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.MiscUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import peersim.graph.Graph;

public class TestUtils {

	public static ByteArrayInputStream blob(Graph g) {
		ByteArrayOutputStream oStream = new ByteArrayOutputStream();
		try {
			new ByteGraphEncoder(oStream).encode(g, IDMapper.IDENTITY);
		} catch (IOException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
		return new ByteArrayInputStream(oStream.toByteArray());
	}
	
	public static ByteArrayInputStream encode(String graph) {
		return runTransformer(new Adj2ByteGraph(), new ByteArrayInputStream(graph.getBytes()));
	}
	
	public static ByteArrayInputStream undirect(ByteArrayInputStream input) {
		return runTransformer(new Undirect(ByteGraphDecoder.class.getName()), input);
	}
	
	public static LightweightStaticGraph graph(String adjList) {
		try {
			AdjListGraphDecoder decoder = new AdjListGraphDecoder(new ByteArrayInputStream(adjList.getBytes()));
			return LightweightStaticGraph.load(decoder);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
		
	public static ByteArrayInputStream runTransformer(ITransformer transformer, ByteArrayInputStream input) {
		try {
			ByteArrayOutputStream ost = new ByteArrayOutputStream();
			transformer.execute(input, ost);
			return new ByteArrayInputStream(ost.toByteArray());
		}catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

}
