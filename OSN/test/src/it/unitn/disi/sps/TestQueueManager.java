package it.unitn.disi.sps;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import junit.framework.Assert;

import it.unitn.disi.TestNetworkBuilder;
import it.unitn.disi.TestUtils;
import it.unitn.disi.sps.QueueManager;
import it.unitn.disi.sps.View;

import org.junit.Before;
import org.junit.Test;

import peersim.core.Node;

public class TestQueueManager {
	
	private Random r = new Random(42);
	
	private TestNetworkBuilder builder;
	
	@Before
	public void initialize() {
		builder = new TestNetworkBuilder();
	}
	
	@Test
	public void testManager() {
		View view = new View(25, r);
		Node [] array = builder.mkNodeArray(20);
		
		for (int i = 0; i < 10; i++) {
			view.append(array[i], r.nextInt());
		}
		
		QueueManager mgr = new QueueManager(view);
		Set<Node> expected = new HashSet<Node>();
		
		mgr.update();
		
		for (int i = 0; i < 10; i++) {
			Assert.assertEquals(array[i], view.getNode(i));
			if (i < 5) {
				Assert.assertEquals(array[i], mgr.popFirst());
			}
			view.append(array[10 + i], r.nextInt());
			expected.add(array[10 + i]);
		}
		
		view.permute();
		mgr.update();
		
		for (int i = 5; i < 10; i++) {
			Assert.assertEquals(array[i], mgr.popFirst());
		}
		
		for (int i = 10; i < 20; i++) {
			Node node = mgr.popFirst();
			Assert.assertTrue(expected.contains(node));
			expected.remove(node);
		}
		
		for (int i = 0; i < 30; i++) {
			Assert.assertNull(mgr.popFirst());
		}
		
		for (int i = 0; i < 30; i++) {
			view.permute();
			mgr.update();
		}
		
		Node special = builder.baseNode();
		view.append(special, 42);
		mgr.update();
		Assert.assertEquals(special, mgr.popFirst());
		Assert.assertNull(mgr.popFirst());
	}
	
	@Test
	public void testShuffling() {
		View view = new View(26, r);
		Node [] array = builder.mkNodeArray(25);
		
		Set<Node> expected = new HashSet<Node>();
		for (int i = 0; i < 20; i++) {
			view.append(array[i], r.nextInt());
			expected.add(array[i]);
		}
		
		QueueManager mgr = new QueueManager(view);
		mgr.update();
		
		for (int i = 0; i < 7; i++) {
			Assert.assertEquals(array[i], view.getNode(i));
			Assert.assertEquals(mgr.popFirst(), view.getNode(i));
			expected.remove(array[i]);
		}
		
		for (int i = 7; i < 20; i++) {
			mgr.permute(r);
			Node node = mgr.popFirst();
			Assert.assertTrue(expected.contains(node));
			expected.remove(node);
		}
		
		for (int i = 0; i < 30; i++) {
			Assert.assertNull(mgr.popFirst());
			mgr.permute(r);
		}
		
		for (int i = 20; i < 25; i++) {
			view.append(array[i], r.nextInt());
			expected.add(array[i]);
		}
		
		mgr.update();
		
		for (int i = 20; i < 25; i++) {
			mgr.permute(r);
			Node node = mgr.popFirst();
			Assert.assertTrue(expected.contains(node));
			expected.remove(node);
		}
		
		Assert.assertNull(mgr.popFirst());
		Assert.assertTrue(expected.isEmpty());
		
		Node special = builder.baseNode();
		view.append(special, 42);
		mgr.update();
		mgr.permute(r);
		Assert.assertEquals(special, mgr.popFirst());
		Assert.assertNull(mgr.popFirst());
	}
}
