package it.unitn.disi.graph;

import gnu.trove.list.array.TIntArrayList;
import it.unitn.disi.graph.BFSIterable.BFSIterator;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.test.framework.TestUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.streams.ResettableFileInputStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import peersim.graph.BitMatrixGraph;
import peersim.graph.GraphFactory;

public class LSGTransformsTest {

	private BitMatrixGraph originalRandom;

	private LightweightStaticGraph random;

	@Test
	public void testLoad() throws Exception {
		init(true, 100);
		for (int i = 0; i < random.size(); i++) {
			Assert.assertEquals(originalRandom.degree(i), random.degree(i));
			for (int j : random.getNeighbours(i)) {
				Assert.assertTrue(random.isEdge(i, j));
			}
		}
	}

	@Test
	public void testUndirect() throws Exception {
		init(true, 100);
		LightweightStaticGraph undir = LightweightStaticGraph.undirect(random);

		int[] expectedDegree = new int[undir.size()];
		for (int i = 0; i < random.size(); i++) {
			for (int j : random.getNeighbours(i)) {
				expectedDegree[i]++;
				Assert.assertTrue(edge(i, j, false), undir.isEdge(i, j));
				Assert.assertTrue(edge(j, i, true), undir.isEdge(j, i));
				if (!random.isEdge(j, i)) {
					expectedDegree[j]++;
				}
			}
		}

		for (int i = 0; i < undir.size(); i++) {
			Assert.assertEquals(Integer.toString(i), expectedDegree[i],
					undir.degree(i));
		}
	}

	@Test
	public void testTransitive() throws Exception {
		init(false, 100);
		IndexedNeighborGraph transitive = LightweightStaticGraph
				.transitiveGraph(random, 2);

		// For each vertex on the graph, the two-hop neighborhood should
		// correspond
		// to the result of an order 2 BFS.
		for (int i = 0; i < transitive.size(); i++) {
			List<Integer> referenceList = neighborhood(random, i, 2);
			Collection<Integer> generatedList = transitive.getNeighbours(i);

			Assert.assertEquals(referenceList.size(), generatedList.size());

			Set<Integer> referenceSet = new HashSet<Integer>(referenceList);
			Set<Integer> generatedSet = new HashSet<Integer>(generatedList);

			Assert.assertTrue(referenceSet.containsAll(generatedSet));
			Assert.assertTrue(generatedSet.containsAll(referenceSet));
		}
	}
	
	@Test
	public void testTrivialSubgraph() throws Exception {
		LightweightStaticGraph graph = LightweightStaticGraph.fromAdjacency(
				new int[][]{
						{1},
						{0}
				});
		
		LightweightStaticGraph single = LightweightStaticGraph.subgraph(graph, 1);
		Assert.assertEquals(1, single.size());
		Assert.assertEquals(0, single.edgeCount());
		Assert.assertEquals(0, single.fastGetNeighbours(0).length);
	}


	@Test
	public void testSubgraph() throws Exception {
		LightweightStaticGraph large = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(
						new ResettableFileInputStream(new File(ClassLoader
								.getSystemResource("Large.bin").toURI())),
						ByteGraphDecoder.class.getName()));

		for (int i = 0; i < large.size(); i++) {
			TIntArrayList list = new TIntArrayList();
			list.add(i);
			list.addAll(large.getNeighbours(i));
			LightweightStaticGraph subgraph = LightweightStaticGraph.subgraph(
					large, list.toArray());

			// Asserts isomorphism.
			isomorphic(large, subgraph, list);
		}
	}

	private void isomorphic(LightweightStaticGraph large,
			LightweightStaticGraph subgraph, TIntArrayList list) {

		SubgraphDecorator decorator = new SubgraphDecorator(large, false);
		decorator.setVertexList(list.toArray());

		for (int i = 0; i < subgraph.size(); i++) {
			Assert.assertEquals(decorator.degree(i), subgraph.degree(i));
			Set<Integer> ref = new HashSet<Integer>();
			Set<Integer> sub = new HashSet<Integer>();
			ref.addAll(decorator.getNeighbours(i));
			sub.addAll(subgraph.getNeighbours(i));
			Assert.assertTrue(ref.equals(sub));
		}
	}

	private List<Integer> neighborhood(IndexedNeighborGraph graph, int root,
			int order) {
		BFSIterator iterator = new BFSIterator(graph, root);
		List<Integer> neighbors = new ArrayList<Integer>();
		// Skips the root.
		iterator.next();
		while (iterator.hasNext()) {
			Pair<Integer, Integer> next = iterator.next();
			if (next.b > order) {
				break;
			}
			neighbors.add(next.a);
		}
		return neighbors;
	}

	private void init(boolean directed, int size) throws IOException {
		originalRandom = new BitMatrixGraph(size, directed);
		GraphFactory.wireKOut(originalRandom, 5, new Random(42));
		ByteArrayInputStream blob = TestUtils.blob(originalRandom);
		random = LightweightStaticGraph.load(new ByteGraphDecoder(blob));
	}

	private String edge(int i, int j, boolean added) {
		return (added ? "MISSING REVERSE:" : "MISSING:") + " (" + i + ", " + j
				+ ")";
	}
}
