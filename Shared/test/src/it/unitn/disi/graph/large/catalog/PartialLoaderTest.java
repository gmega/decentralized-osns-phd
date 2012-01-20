package it.unitn.disi.graph.large.catalog;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

public class PartialLoaderTest extends CatalogTest {

	private static final int RANDOM = 20000;

	@Test
	public void testPartiaLoads() throws Exception {
		CatalogReader reader = new CatalogReader(new ByteArrayInputStream(
				fIndex), CatalogRecordTypes.PROPERTY_RECORD);
		PartialLoader loader = new PartialLoader(reader,
				ByteGraphDecoder.class, fFile);

		loader.start(null);

		Random rnd = new Random();

		// Tries to load a certain number of random neighborhoods.
		for (int i = 0; i < RANDOM; i++) {
			int id = rnd.nextInt(fOriginal.size());
			IndexedNeighborGraph neighborhood = loader.subgraph(id);
			int ids[] = loader.verticesOf(id);
			Assert.assertEquals(neighborhood.size(), fOriginal.degree(id) + 1);
			for (int j = 0; j < neighborhood.size(); j++) {
				assertSameNeighbors(neighborhood, fOriginal, j, ids);
			}
		}

		loader.stop();
	}

	private void assertSameNeighbors(IndexedNeighborGraph loaded,
			IndexedNeighborGraph original, int lRoot, int[] ids) {

		Set<Integer> filter = new HashSet<Integer>();
		for (int j = 0; j < ids.length; j++) {
			filter.add(ids[j]);
		}

		// Collects mapped ids of the loaded graph into a set.
		Set<Integer> lVertices = new HashSet<Integer>();
		for (int i = 0; i < loaded.degree(lRoot); i++) {
			lVertices.add(ids[loaded.getNeighbor(lRoot, i)]);
		}

		// Collects ids of the original graph into a set.
		Set<Integer> oVertices = new HashSet<Integer>();
		int oRoot = ids[lRoot];
		for (int i = 0; i < original.degree(oRoot); i++) {
			int neighbor = original.getNeighbor(oRoot, i);
			if (filter.contains(neighbor)) {
				oVertices.add(neighbor);
			}
		}

		Assert.assertEquals(oVertices.size(), lVertices.size());
		Assert.assertTrue(oVertices.containsAll(lVertices));
		Assert.assertTrue(lVertices.containsAll(oVertices));
	}

}
