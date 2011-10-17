package it.unitn.disi.unitsim;

import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;

/**
 * {@link NeighborhoodLoader} is an {@link IGraphProvider} which allows loading
 * of individual neighborhoods of an underlying, possibly huge graph. Subgraph
 * ids are intepreted as node IDs in the underlying graph, for which
 * neighborhoods can be accessed one at a time.<BR>
 * <BR>
 * Note: the current implementation simply loads it all into memory, meaning it
 * doesn't really provide any memory savings graph-wise.
 * 
 * @author giuliano
 */
@AutoConfig
public class NeighborhoodLoader implements IPlugin, IGraphProvider {

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

	@Override
	public int size() {
		return fGraph.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.unitn.disi.unitsim.INeighborhoodLoader#neighborhood(java.lang.Integer)
	 */
	@Override
	public LightweightStaticGraph subgraph(Integer node) {
		return LightweightStaticGraph.subgraph(fGraph, verticesOf(node));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * it.unitn.disi.unitsim.INeighborhoodLoader#verticesOf(java.lang.Integer)
	 */
	@Override
	public int[] verticesOf(Integer node) {
		int[] neighbors = fGraph.fastGetNeighbours(node);
		int[] vertices = new int[neighbors.length + 1];
		vertices[0] = node;
		System.arraycopy(neighbors, 0, vertices, 1, neighbors.length);
		return vertices;
	}

}
