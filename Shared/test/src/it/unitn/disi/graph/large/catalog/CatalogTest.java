package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.BeforeClass;

public class CatalogTest {

	protected static File fFile;

	protected static IndexedNeighborGraph fOriginal;

	protected static byte[] fIndex;

	protected static byte[] fRewritten;

	@BeforeClass
	public static void setUp() throws Exception {
		// fFile = new File(CatalogTest.class.getClassLoader()
		// .getResource("Large.bin").toURI());
		fFile = new File("/home/giuliano/Graphs/Facebook.bin");
		fOriginal = LightweightStaticGraph.load(new ByteGraphDecoder(
				new ResettableFileInputStream(fFile)));
		// GraphIndexer indexer = new GraphIndexer(
		// CatalogRecordTypes.PROPERTY_RECORD);
		// ByteArrayOutputStream index = new ByteArrayOutputStream();
		// ByteArrayOutputStream indexedGraph = new ByteArrayOutputStream();
		// indexer.indexGraph(fOriginal, index, indexedGraph);

		fIndex = load(new File("/home/giuliano/Graphs/Facebook-catalog.bin"))
				.toByteArray();
		fRewritten = load(new File("/home/giuliano/Graphs/Facebook-catalogorder.bin"))
				.toByteArray();
	}

	private static ByteArrayOutputStream load(File file) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				file));

		int b;
		while ((b = is.read()) != -1) {
			buf.write(b);
		}
		
		return buf;
	}
}
