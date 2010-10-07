package it.unitn.disi.graph;

import it.unitn.disi.codecs.GraphCodecHelper;
import it.unitn.disi.codecs.ResettableGraphDecoder;
import it.unitn.disi.utils.ResettableFileInputStream;
import it.unitn.disi.utils.graph.IndexedNeighborGraph;
import it.unitn.disi.utils.graph.LightweightStaticGraph;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class GraphProtocolInit implements Control {

	// --------------------------------------------------------------------------
	// Parameters
	// --------------------------------------------------------------------------
	
	/**
	 * The protocol to operate on.
	 * 
	 * @config
	 */
	@Attribute
	private int protocol;
	
	/**
	 * The file containing the graph.
	 */
	@Attribute
	private String file;

	/**
	 * The {@link ResettableGraphDecoder} used to decode the graph format.
	 */
	@Attribute(defaultValue = "it.unitn.disi.codecs.AdjListGraphDecoder")
	private String decoder;
	
	/**
	 * Whether or not the graph is undirected.
	 */
	@Attribute
	private boolean undirected;
	
	// --------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------

	public boolean execute() {
		IndexedNeighborGraph graph;
		try {
			graph = loadGraph(GraphCodecHelper.createDecoder(file, decoder));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}

		INodeRegistry registry = NodeRegistry.getInstance();
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			GraphProtocol gp = (GraphProtocol) node.getProtocol(protocol);
			gp.configure(node, graph, registry);
		}

		return false;
	}
	
	private IndexedNeighborGraph loadGraph(ResettableGraphDecoder decoder) throws IOException{
		LightweightStaticGraph graph = LightweightStaticGraph.load(decoder);
		return transform(graph);
	}
	
	protected LightweightStaticGraph transform(LightweightStaticGraph graph) {
		if (undirected) {
			return LightweightStaticGraph.undirect(graph);
		}
		return graph;
	}
}

