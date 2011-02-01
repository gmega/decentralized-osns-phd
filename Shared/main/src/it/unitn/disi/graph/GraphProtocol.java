package it.unitn.disi.graph;

import it.unitn.disi.utils.IInputStreamProvider;
import it.unitn.disi.utils.peersim.IDynamicLinkable;
import it.unitn.disi.utils.peersim.INodeRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;

/**
 * Provides a efficient {@link Protocol} implementation which is, to the maximum
 * possible extent, detached from the state of the {@link Network} singleton. <BR>
 * <BR>
 * The main point here is that we want to preload a graph which will tell us
 * information about the network, but without having to instantiate it. For
 * example, we want to be able to say that the {@link Node} with ID 25 is
 * friends with nodes 1, 3, and 5 before an instance of {@link Node} such that
 * <code>{@link Node#getID()} == 25</code> even exists. <BR>
 * <BR>
 * The whole problem is that this is made remarkably hard by the fact that the
 * {@link Linkable} interface is filled with methods that work with {@link Node}
 * instances. <BR>
 * <BR>
 * The alternative to preloading would be reading such information from disk
 * incrementally as nodes are created. This is, however, not simple to achieve.<BR>
 * <BR>
 * In practial terms, these things mean that operations such as
 * {@link #degree()} will refer to the chosen graph, and not the network.
 * Moreover, it means that operations such as {@link #getNeighbor(int)} might
 * return <code>null</code> at some point in the execution, but not at the
 * other.<BR>
 * 
 * @author giuliano
 */
public class GraphProtocol implements Protocol, IDynamicLinkable {

	private INodeRegistry fRegistry;

	private IndexedNeighborGraph fGraph;

	private int fId = -1;

	public GraphProtocol() {
	}

	public GraphProtocol(String prefix) {
	}

	/**
	 * This method should be called once, by an initializing controller. I'm not
	 * very fond of doing things this way, but PeerSim is the one doing the
	 * instantiation, and we cannot access dependencies at construction time.
	 * 
	 * @param node
	 *            the {@link Node} instance logically bound to this protocol
	 *            instance.
	 * 
	 * @param graph
	 *            the {@link IndexedNeighborGraph} providing neighbourhood
	 *            information for this protocol.
	 * 
	 * @param registry
	 *            an {@link INodeRegistry} which lets us map the neighbors of
	 *            the bound node into {@link Node} instances.
	 */
	public void configure(Node node, IndexedNeighborGraph graph,
			INodeRegistry registry) {
		this.configure((int) node.getID(), graph, registry);
	}

	/**
	 * Alternative formulation for
	 * {@link #configure(Node, IndexedNeighborGraph, INodeRegistry)}, where
	 * instead of passing the actual {@link Node} object, the client passes only
	 * its numeric ID (as given by {@link Node#getID()}, and cast to
	 * <code>int</code>). Clients should avoid using this method, since it is
	 * more error prone, and less abstract.
	 */
	public void configure(int nodeId, IndexedNeighborGraph graph,
			INodeRegistry registry) {

		if (isInit()) {
			throw new IllegalStateException(
					"Initialization should be performed only once.");
		}

		if (nodeId < 0) {
			throw new IllegalArgumentException();
		}
		
		fId = nodeId;
		if (graph == null || registry == null) {
			throw new NullPointerException();
		}

		fGraph = graph;
		fRegistry = registry;
	}

	// ----------------------------------------------------------------------
	// Linkable interface.
	// ----------------------------------------------------------------------

	public boolean contains(Node neighbor) {
		return fGraph.isEdge(fId, (int) neighbor.getID());
	}

	public int degree() {
		return fGraph.degree(fId);
	}

	public Node getNeighbor(int i) {
		return fRegistry.getNode(fGraph.getNeighbor(fId, i));
	}

	@Override
	public boolean hasChanged(int time) {
		// Never changes.
		return false;
	}

	@Override
	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException("Linkable is read-only.");
	}

	@Override
	public void pack() {
	}

	@Override
	public void onKill() {
		fGraph = null;
		fRegistry = null;
	}

	// ----------------------------------------------------------------------
	// Cloneable requirements.
	// ----------------------------------------------------------------------

	public Object clone() {
		try {
			return (GraphProtocol) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	public int getId() {
		chkInit();
		return (int) fId;
	}

	public IndexedNeighborGraph graph() {
		chkInit();
		return fGraph;
	}

	private void chkInit() {
		if (!isInit()) {
			throw new IllegalStateException(
					"GraphProtocol hasn't been initialized properly.");
		}
	}

	private boolean isInit() {
		return !(fId == -1 || fGraph == null || fRegistry == null);
	}
}

class FileInputStreamProvider implements IInputStreamProvider {
	private String fFile;

	public FileInputStreamProvider(String file) {
		fFile = file;
	}

	public InputStream get() throws IOException {
		return new FileInputStream(new File(fFile));
	}

	public Object clone() {
		return this;
	}
}
