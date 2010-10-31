package it.unitn.disi.sps.newscast;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import it.unitn.disi.sps.selectors.RandomSelector;
import it.unitn.disi.sps.selectors.TabooSelectionFilter;
import it.unitn.disi.test.framework.TestNetworkBuilder;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import peersim.core.Node;

public class TestPeerSelectors {
	@Test public void testTabooedSelector() throws Exception {
		
		int SIZE = 50;
		
		Random r = new Random(42);
		
		IView view = new View(SIZE, r);
		
		Set<Node> taboo = new HashSet<Node>();
		
		TestNetworkBuilder builder = new TestNetworkBuilder();
		
		for (int i = 0; i < SIZE; i++) {
			view.append(builder.baseNode(), r.nextInt(1000));
		}
		
		builder.replayAll();
		
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
