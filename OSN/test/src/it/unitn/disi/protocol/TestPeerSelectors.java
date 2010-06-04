package it.unitn.disi.protocol;

import static org.junit.Assert.*;
import it.unitn.disi.TestUtils;
import it.unitn.disi.protocol.selectors.RandomSelector;
import it.unitn.disi.protocol.selectors.TabooSelectionFilter;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import junit.framework.TestCase;
import peersim.core.Node;

public class TestPeerSelectors{
	@Test public void testTabooedSelector() throws Exception {
		
		int SIZE = 50;
		
		Random r = new Random(42);
		
		IView view = new View(SIZE, r);
		
		Set<Node> taboo = new HashSet<Node>();
		
		for (int i = 0; i < SIZE; i++) {
			view.append(TestUtils.makeNode(), r.nextInt(1000));
		}
		
		RandomSelector tabooed = new RandomSelector(view, r);
		TabooSelectionFilter filter = new TabooSelectionFilter(25);
		
		for (int i = 0; i < SIZE; i++) {
			if (i == 24) {
				taboo = new HashSet<Node>();
			}
			
			Node result = tabooed.selectPeer(filter);
			assertNotNull(result);
			assertFalse(taboo.contains(result));
			taboo.add(result);
		}
		

		filter = new TabooSelectionFilter(50);
		taboo = new HashSet<Node>();
		for (int i = 0; i < SIZE; i++) {
			Node result = tabooed.selectPeer(filter);
			assertNotNull(result);
			assertFalse(taboo.contains(result));
			taboo.add(result);
		}
		
		assertNotNull(tabooed.selectPeer(filter));
	}
}
