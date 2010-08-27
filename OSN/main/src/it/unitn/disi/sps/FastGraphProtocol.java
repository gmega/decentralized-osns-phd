package it.unitn.disi.sps;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.codecs.AdjListGraphDecoder;
import it.unitn.disi.codecs.ByteGraphDecoder;
import it.unitn.disi.codecs.GraphDecoder;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.graph.Graph;

/**
 * Provides an efficient {@link Protocol} implementation which is, to the
 * maximum possible extent, detached from the state of the {@link Network}
 * singleton. <BR>
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
 * In practial terms, these things means that operations such as
 * {@link #degree()} will refer to the chosen graph, and not the network.
 * Moreover, it means that operations such as {@link #getNeighbor(int)} might
 * return <code>null</code> at some point in the execution, but not at the
 * other.<BR>
 * 
 * @author giuliano
 */
public class FastGraphProtocol implements Protocol, IDynamicLinkable {

	private static final String PAR_FILE = "file";

	private static final String PAR_ENCODING = "encoding";

	private static final String PAR_UNDIR = "undir";

	public static final String ADJACENCY = "adjacency";

	public static final String BINARY_ST = "longpairs";

	/** Shared immutable graph. **/
	private static GraphEntry[] fGraphEntries;

	private static final INodeRegistry fReg = NodeRegistry.getInstance();

	private static boolean fUndir;

	private GraphEntry fEntry;

	private long fId = -1;

	private String fEncoding;

	private IInputStreamProvider fProvider;

	public FastGraphProtocol(String prefix) {
		this(new FileInputStreamProvider(Configuration.getString(prefix + "."
				+ PAR_FILE)), Configuration.contains(prefix + "." + PAR_UNDIR),
				Configuration.getString(prefix + "." + PAR_ENCODING));
	}

	public FastGraphProtocol(IInputStreamProvider provider, boolean undir,
			String encoding) {
		fProvider = provider;
		fEncoding = encoding;
		fUndir = undir;
	}

	/**
	 * Binds a node to the current {@link FastGraphProtocol} instance. The
	 * binding procedure consists of looking up a node in the preloaded graph
	 * which has an ID equal to {@link Node#getID()}.
	 * 
	 * Note that this translates into some pretty heavy assumptions.
	 * Particularly, it assumes that the ID space of the graph is the same as
	 * the ID space of the network. This may also have funny consequences to
	 * growing networks.
	 */
	public void bind(Node node) {
		try {
			fId = node.getID();
			fEntry = entry();
		} catch (ArrayIndexOutOfBoundsException ex) {
			throw new IllegalStateException("Node has an ID  (" + node.getID()
					+ ") larger than the size of the preloaded graph ("
					+ fGraphEntries.length + "). One possible cause is that your " +
							"target network size might be larger than the actual size" +
							" of the preloaded graph.");
		}
	}

	public Object clone() {
		try {
			FastGraphProtocol sn = (FastGraphProtocol) super.clone();

			sn.fProvider = (IInputStreamProvider) fProvider.clone();
			sn.fId = fId;

			return sn;
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean contains(Node neighbor) {
		return fEntry.contains((int) neighbor.getID());
	}

	public Node getNeighbor(int i) {
		return fReg.getNode(fEntry.fAdjList.get(i));
	}

	public int degree() {
		return fEntry.degree();
	}

	public int getId() {
		if (fId == -1) {
			throw new IllegalStateException("Instance not yet bound.");
		}
		return (int) fId;
	}

	public ArrayList<Integer> getNeighbors() {
		return fEntry.fAdjList;
	}

	private GraphEntry entry() {
		ensureInit();
		return fGraphEntries[(int) getId()];
	}

	private void ensureInit() {
		if (fGraphEntries != null) {
			return;
		}

		InputStream is = null;
		try {
			is = fProvider.get();
			fGraphEntries = createGraph(getDecoder(is));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			MiscUtils.safeClose(is, false);
		}

	}

	private GraphDecoder getDecoder(InputStream is) throws IOException {
		if (fEncoding.equals(ADJACENCY)) {
			return new AdjListGraphDecoder(is);
		} else {
			return new ByteGraphDecoder(is);
		}
	}

	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException("Social Network is read-only.");
	}

	public boolean hasChanged(int time) {
		/** Always returns false as FastGraphProtocol is read-only. */
		return false;
	}

	public void pack() {
	}

	public void onKill() {
	}

	public Graph getGraph() {
		ensureInit();
		return GRAPH_WRAPPER;
	}

	private static GraphEntry[] createGraph(GraphDecoder it)
			throws IOException {
		ArrayList<GraphEntry> entries = new ArrayList<GraphEntry>();
		
		while (it.hasNext()) {
			int source = it.getSource();
			int target = it.next();
			getCreate(source, entries).add((int) target);
			if (fUndir) {
				getCreate(target, entries).add((int) source);
			}
		}
		
		return entries.toArray(new GraphEntry[entries.size()]);
	}
	
	private static GraphEntry getCreate(int id, ArrayList<GraphEntry> entries) {
		while (entries.size() <= id) {
			entries.add(new GraphEntry());
		}
		
		return entries.get(id);
	}

	private static Graph GRAPH_WRAPPER = new Graph() {

		public int degree(int i) {
			return fGraphEntries[i].degree();
		}

		public boolean directed() {
			return false;
		}

		public Collection<Integer> getNeighbours(int i) {
			return fGraphEntries[i].getNeighbors();
		}

		public Object getNode(int i) {
			INodeRegistry nr = NodeRegistry.getInstance();
			if (nr.contains(i)) {
				return nr.getNode(i);
			}

			return null;
		}

		public boolean isEdge(int i, int j) {
			return fGraphEntries[i].contains(j);
		}

		public int size() {
			return fGraphEntries.length;
		}

		public Object getEdge(int i, int j) {
			return null;
		}

		public boolean clearEdge(int i, int j) {
			return false;
		}

		public boolean setEdge(int i, int j) {
			return false;
		}
	};

	static class GraphEntry {
		ArrayList<Integer> fAdjList;
		HashSet<Integer> fAdjSet;
		List<Integer> fRoAdjList;

		private int fSize;

		public GraphEntry() {
			fAdjList = new ArrayList<Integer>();
			fRoAdjList = Collections.unmodifiableList(fAdjList);

			fAdjSet = new HashSet<Integer>();
			fSize = 0;
		}

		public void add(Integer i) {
			if (!contains(i)) {
				fAdjList.add(i);
				fAdjSet.add(i);
				fSize++;
			}
		}

		public List<Integer> getNeighbors() {
			return fRoAdjList;
		}

		public boolean contains(Integer i) {
			return fAdjSet.contains(i);
		}

		public int getNeighbor(Integer i) {
			return fAdjList.get(i.intValue());
		}

		public int degree() {
			return fSize;
		}
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
