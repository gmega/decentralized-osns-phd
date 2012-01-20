package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.SparseIDMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import peersim.graph.GraphAlgorithms;
import peersim.graph.NeighbourListGraph;

public class CatalogReadsTest extends CatalogTest {

	@Test
	public void storesRightDegrees() throws Exception {
		CatalogReader reader = new CatalogReader(new ByteArrayInputStream(
				fIndex), CatalogRecordTypes.PROPERTY_RECORD);
		int count = 0;
		while (reader.hasNext()) {
			count++;
			reader.next();
			int root = reader.get("root").intValue();
			int degree = reader.get("size").intValue();
			Assert.assertEquals(fOriginal.degree(root), degree);
		}

		Assert.assertEquals(fOriginal.size(), count);
	}

	@Test
	public void sequentialRead() throws Exception {
		CatalogRecord[] records = loadCatalog();
		// Sorts by offset.
		Arrays.sort(records, new Comparator<CatalogRecord>() {
			@Override
			public int compare(CatalogRecord o1, CatalogRecord o2) {
				return (int) (o1.offset - o2.offset);
			}
		});

		ByteArrayInputStream stream = new ByteArrayInputStream(fRewritten);
		ByteGraphDecoder decoder = new ByteGraphDecoder(stream);
		for (CatalogRecord record : records) {
			// Positions the stream under the offset.
			stream.reset();
			stream.skip(record.offset / Byte.SIZE);

			// Reads.
			decoder.realign();
			int source = decoder.getSource();
			// Should be the right source.
			Assert.assertEquals(record.root, source);
			// Reads the rest.
			Set<Integer> neighbors = new HashSet<Integer>();
			while (source == decoder.getSource()) {
				int neighbor = decoder.next();
				Assert.assertFalse(neighbors.contains(neighbor));
				neighbors.add(neighbor);
			}
			// Should be the same as in the original graph.
			Set<Integer> reference = new HashSet<Integer>();
			reference.addAll(fOriginal.getNeighbours(source));

			Assert.assertTrue(reference.containsAll(neighbors));
			Assert.assertTrue(neighbors.containsAll(reference));
		}
	}

	@Test
	public void randomRead() throws Exception {
		// Picks 1000 random neighborhoods from the catalog.
		CatalogRecord[] records = loadCatalog();
		Random rand = new Random(42);
		ByteArrayInputStream stream = new ByteArrayInputStream(fRewritten);
		ByteGraphDecoder decoder = new ByteGraphDecoder(stream);
		for (int i = 0; i < 1000; i++) {
			CatalogRecord random = records[rand.nextInt(records.length)];
			// Discards trivial records.
			if (random.size == 1) {
				i--;
				continue;
			}
			// Reads the neighborhoods from the reordered graph.
			NeighbourListGraph neighborhood = readNeighborhood(records,
					decoder, stream, random);
			LightweightStaticGraph g = LightweightStaticGraph
					.fromGraph(neighborhood);
			Assert.assertEquals(random.clustering,
					GraphAlgorithms.clustering(g, 0));
		}
	}

	private CatalogRecord[] loadCatalog() {
		ByteArrayInputStream stream = new ByteArrayInputStream(fIndex);
		ArrayList<CatalogRecord> index = new ArrayList<CatalogRecord>();

		CatalogReader reader = new CatalogReader(stream,
				CatalogRecordTypes.PROPERTY_RECORD);

		while (reader.hasNext()) {
			reader.next();
			index.add(new CatalogRecord(reader));
		}

		return index.toArray(new CatalogRecord[index.size()]);
	}

	private NeighbourListGraph readNeighborhood(CatalogRecord[] catalog,
			ByteGraphDecoder decoder, InputStream stream,
			CatalogRecord reference) throws IOException {

		NeighbourListGraph graph = new NeighbourListGraph(reference.size + 1,
				true);
		AbstractIDMapper mapper = new SparseIDMapper();

		// First reads the immediate neighbors.
		readAdd(graph, mapper, stream, decoder, reference, false);

		// Now read the inter-neighbor connections.
		for (int neighbor : graph.getNeighbours(mapper.map(reference.root))) {
			readAdd(graph, mapper, stream, decoder,
					findRecord(catalog, mapper.reverseMap(neighbor)), true);
		}

		Assert.assertEquals(0, mapper.map(reference.root));

		return graph;
	}

	private void readAdd(NeighbourListGraph graph, AbstractIDMapper mapper,
			InputStream stream, ByteGraphDecoder decoder,
			CatalogRecord reference, boolean constrain) throws IOException {
		stream.reset();
		long toSkip = reference.offset / Byte.SIZE;
		long skipped = stream.skip(toSkip);
		Assert.assertEquals(toSkip, skipped);
		decoder.realign();
		while (decoder.getSource() == reference.root) {
			int i = mapper.addMapping(decoder.getSource());
			int j = decoder.next();
			if (constrain && !mapper.isMapped(j)) {
				continue;
			}
			j = mapper.addMapping(j);
			if (graph.isEdge(i, j)) {
				Assert.fail();
			}
			graph.setEdge(i, j);
		}
	}

	private CatalogRecord findRecord(CatalogRecord[] catalog, int root) {
		for (CatalogRecord record : catalog) {
			if (record.root == root) {
				return record;
			}
		}

		throw new NoSuchElementException();
	}

	class CatalogRecord {
		int root;
		int size;
		double clustering;
		long offset;

		public CatalogRecord(ICatalogCursor source) {
			root = source.get("root").intValue();
			size = source.get("size").intValue();
			clustering = source.get("clustering").doubleValue();
			offset = source.get("offset").longValue();
		}
	}

}
