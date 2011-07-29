package it.unitn.disi.unitsim;

import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;

/**
 * Plug-in which provides social neighborhoods on-demand. The current
 * implementation simply loads it all into memory and subgraphs as requests
 * come.
 * 
 * @author giuliano
 */
@AutoConfig
public class NeighborhoodLoader implements IPlugin {

	@Attribute("decoder")
	private String fDec;

	@Attribute("file")
	private String fFile;

	private LightweightStaticGraph fGraph;

	@Override
	public String id() {
		return NeighborhoodLoader.class.getSimpleName();
	}

	@Override
	public void start(IResolver resolver) throws Exception {
		fGraph = LightweightStaticGraph.load(GraphCodecHelper.createDecoder(
				fFile, fDec));
	}

	@Override
	public void stop() throws Exception {
		fGraph = null;
	}

	public LightweightStaticGraph neighborhood(Integer node) {
		return LightweightStaticGraph.subgraph(fGraph, verticesOf(node));
	}

	public int[] verticesOf(Integer node) {
		int[] neighbors = fGraph.fastGetNeighbours(node);
		int[] vertices = new int[neighbors.length + 1];
		vertices[0] = node;
		System.arraycopy(neighbors, 0, vertices, 1, neighbors.length);
		return vertices;
	}

}
