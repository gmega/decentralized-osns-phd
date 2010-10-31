package it.unitn.disi.util.peersim;

import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.peersim.PermutingCache;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import peersim.core.Node;

public class PermutingCacheTest extends PeerSimTest {
	
	int linkable;
	Node hub;
	
	@Before
	public void setUp() {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.mkNodeArray(10);
		hub = builder.getNodes().get(0);
		
		linkable = builder.assignLinkable(new long [][]{
				{1, 2, 3, 4, 5, 6, 7, 8, 9},
				{0},{0},{0},{0},{0},{0},{0},{0},{0}
		});
		
		builder.replayAll();
	}

	@Test
	public void basePermutation() {
		PermutingCache cache = new PermutingCache(linkable);
		cache.populate(hub);
		Assert.assertEquals(9, cache.size());
		
		for (int i = 0; i < 50; i++) {
			cache.shuffle();
			assertContains(cache, 1, 2, 3, 4, 5, 6, 7, 8, 9);
		}
	}
	
	@Test
	public void filteredNeighborhood() {
		ISelectionFilter oddFilter = new ISelectionFilter() {
			@Override
			public boolean canSelect(Node node) {
				return (node.getID() % 2) == 0;
			}
			
			@Override
			public Node selected(Node node) {
				return node;
			}
		};
		
		PermutingCache cache = new PermutingCache(linkable);
		cache.populate(hub, oddFilter);
		assertContains(cache, 2, 4, 6, 8);
	}
	
	@Test
	public void filteredToEmptynessNeighborhood() {
		ISelectionFilter nayFilter = new ISelectionFilter() {
			
			@Override
			public Node selected(Node node) {
				return node;
			}
			
			@Override
			public boolean canSelect(Node node) {
				return false;
			}
		};
		
		PermutingCache cache = new PermutingCache(linkable);
		cache.populate(hub, nayFilter);
		Assert.assertEquals(0, cache.size());
	}
	
	
	private void assertContains(PermutingCache cache, int...elements) {
		Set<Integer> contains = new HashSet<Integer>();
		Set<Integer> shouldContain = new HashSet<Integer>();
		
		for (int i = 0; i < elements.length; i++) {
			shouldContain.add(elements[i]);
		}
		
		for (int i = 0; i < cache.size(); i++) {
			contains.add((int)cache.get(i).getID());
		}
		
		Assert.assertTrue(contains.containsAll(shouldContain));
		Assert.assertTrue(shouldContain.containsAll(contains));
	}
}
