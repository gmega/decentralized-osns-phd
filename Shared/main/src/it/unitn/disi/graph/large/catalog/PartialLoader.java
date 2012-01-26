package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.NullGraph;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.ResettableGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.DenseIDMapper;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import peersim.config.IResolver;
import peersim.config.plugin.IPlugin;
import peersim.graph.Graph;
import peersim.graph.NeighbourListGraph;

/**
 * Partial loader can load cataloged graphs one neighborhood at a time.
 * 
 * @author giuliano
 */
public class PartialLoader implements IGraphProvider, IPlugin {

	private Class<? extends ResettableGraphDecoder> fDecoderClass;

	private ICatalogCursor fCursor;

	private File fGraph;

	private HashMap<Integer, CatalogEntry> fCatalog = new HashMap<Integer, CatalogEntry>();

	private ResettableFileInputStream fStream;

	private ResettableGraphDecoder fDecoder;

	public PartialLoader(ICatalogCursor cursor,
			Class<? extends ResettableGraphDecoder> klass, File graph) {
		fDecoderClass = klass;
		fCursor = cursor;
		fGraph = graph;
	}

	public IndexedNeighborGraph subgraph(Integer root) {
		CatalogEntry entry = catalogEntry(root);
		NeighbourListGraph nlg = new NeighbourListGraph(entry.size + 1, false);

		// Maps the root.
		DenseIDMapper mapper = init(entry, nlg);
		for (int i = 1; i < entry.size; i++) {
			readEdges(catalogEntry(mapper.reverseMap(i)), mapper, nlg, false);
		}

		return LightweightStaticGraph.fromGraph(nlg);
	}

	private CatalogEntry catalogEntry(int id) {
		CatalogEntry entry = fCatalog.get(id);
		if (entry == null) {
			throw new IllegalStateException("No catalog entry for element "
					+ id + ".");
		}
		return entry;
	}

	@Override
	public int size() {
		return fCatalog.size();
	}

	@Override
	public int[] verticesOf(Integer subgraph) {
		DenseIDMapper mapper = init(catalogEntry(subgraph), new NullGraph());
		return mapper.reverseMappings();
	}
	
	public int size(Integer subgraph) {
		return catalogEntry(subgraph).size + 1;
	}

	private DenseIDMapper init(CatalogEntry entry, Graph graph) {
		DenseIDMapper mapper = new DenseIDMapper(graph.size());
		mapper.addMapping(entry.root);
		readEdges(entry, mapper, graph, true);
		return mapper;
	}

	private void readEdges(CatalogEntry entry, DenseIDMapper mapper,
			Graph graph, boolean addMappings) {
		try {
			readEdges0(entry, mapper, graph, addMappings);
		} catch (IOException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	private void readEdges0(CatalogEntry entry, DenseIDMapper mapper,
			Graph graph, boolean addMappings) throws IOException {

		// Positions the stream under the offset.
		fStream.reposition(entry.offset / Byte.SIZE);
		fDecoder.realign();

		// Reads.
		int source = fDecoder.getSource();

		// Little santity check.
		if (source != entry.root) {
			throw new IllegalStateException("Catalog says " + entry.root
					+ " but graph contains " + source + " at offset "
					+ entry.offset + ".");
		}

		int mSource = mapper.map(source);
		while (source == fDecoder.getSource() && fDecoder.hasNext()) {
			int target = fDecoder.next();

			if (addMappings) {
				mapper.addMapping(target);
			}

			// If addMappings not true, this will ignore any
			// unmapped vertices, effectively constraining the
			// edges to the vertices mapped when "addMapping"
			// was true.
			if (mapper.isMapped(target)) {
				target = mapper.map(target);
				graph.setEdge(mSource, target);
			}
		}
	}

	@Override
	public String id() {
		return PartialLoader.class.getSimpleName();
	}

	@Override
	public void start(IResolver resolver) throws Exception {
		while (fCursor.hasNext()) {
			fCursor.next();
			int root = fCursor.get(NeighborhoodRoot.KEY).intValue();
			fCatalog.put(root,
					new CatalogEntry(root, fCursor.get(NeighborhoodSize.KEY)
							.intValue(), fCursor.get("offset").longValue()));
		}

		fStream = new ResettableFileInputStream(fGraph);
		fDecoder = GraphCodecHelper.createDecoder(fDecoderClass, fStream);
	}

	@Override
	public void stop() throws Exception {
		fStream.close();
	}

	static class CatalogEntry {
		public final int root;
		public final int size;
		public final long offset;

		public CatalogEntry(int root, int size, long offset) {
			this.root = root;
			this.size = size;
			this.offset = offset;
		}
	}
}
