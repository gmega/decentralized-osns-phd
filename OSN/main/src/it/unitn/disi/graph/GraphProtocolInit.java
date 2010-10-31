package it.unitn.disi.graph;

import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.io.IOException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class GraphProtocolInit implements Control {
	
	enum Representation {
		ARRAY, BITMATRIX
	}
	
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
	@Attribute(defaultValue = "it.unitn.disi.graph.codecs.AdjListGraphDecoder")
	private String decoder;
	
	/**
	 * Whether or not the graph is undirected.
	 */
	@Attribute
	private boolean undirected;
	
	/**
	 * Which underlying representation to use for the graph.
	 */
	@Attribute
	private String representation;
	
	@Attribute(defaultValue = "-1")
	private int size;
	
	// --------------------------------------------------------------------------
	// Methods
	// --------------------------------------------------------------------------

	public boolean execute() {
		
		System.out.println(this.getClass().getName() + ": using " + file + ".");
		
		IndexedNeighborGraph graph;
		try {
			graph = loadGraph(GraphCodecHelper.createDecoder(file, decoder));
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
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
		switch(Representation.valueOf(representation.toUpperCase())) {
		case ARRAY:
			return arrayGraph(decoder);
		case BITMATRIX:
			return bitmatrixGraph(decoder);
		default:
			throw new IllegalArgumentException("Invalid representation "
					+ representation + ".");
		}
	}
	
	protected BitMatrixGraphAdapter bitmatrixGraph(ResettableGraphDecoder decoder) {
		if (size < 0) {
			throw new IllegalStateException("When using a bit matrix representation, you need to specify the size.");
		}
		BitMatrixGraphAdapter graph = new BitMatrixGraphAdapter(size);
		while (decoder.hasNext()) {
			graph.setEdge(decoder.getSource(), decoder.next());
		}
		return graph;
	}
	
	protected LightweightStaticGraph arrayGraph (ResettableGraphDecoder decoder) throws IOException {
		LightweightStaticGraph graph = LightweightStaticGraph.load(decoder);
		if (undirected) {
			graph = LightweightStaticGraph.undirect(graph);
		}
		return graph;
	}
}

