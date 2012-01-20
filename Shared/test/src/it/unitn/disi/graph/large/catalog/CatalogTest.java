package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.BeforeClass;

public class CatalogTest {
	
	protected static File fFile;

	protected static IndexedNeighborGraph fOriginal;

	protected static byte[] fIndex;

	protected static byte[] fRewritten;

	@BeforeClass
	public static void setUp() throws Exception {
		fFile = new File(CatalogTest.class.getClassLoader()
				.getResource("Large.bin").toURI());
		fOriginal = LightweightStaticGraph.load(new ByteGraphDecoder(
				new ResettableFileInputStream(fFile)));
		GraphIndexer indexer = new GraphIndexer(
				CatalogRecordTypes.PROPERTY_RECORD);
		ByteArrayOutputStream index = new ByteArrayOutputStream();
		ByteArrayOutputStream indexedGraph = new ByteArrayOutputStream();
		indexer.indexGraph(fOriginal, index, indexedGraph);
		fIndex = index.toByteArray();
		fRewritten = indexedGraph.toByteArray();
	}
}
