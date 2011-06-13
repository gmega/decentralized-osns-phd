package it.unitn.disi.util.peersim;

import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.peersim.BitSetNeighborhood;
import junit.framework.Assert;

import org.junit.Test;

import peersim.core.Linkable;
import peersim.core.Node;

public class BitSetNeighborhoodTest extends PeerSimTest{
	
	@Test
	public void testIterationOrdering() throws Exception {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(100);

		int DUMMY_LINKABLE = builder.assignCompleteLinkable();
		builder.done();
		
		Node zero = builder.getNodes().get(0);		
		Linkable staticl = (Linkable) zero.getProtocol(DUMMY_LINKABLE);
		BitSetNeighborhood bsn = new BitSetNeighborhood(staticl);
		
		int[] order = new int[staticl.degree()];
		for (int i = 0; i < staticl.degree(); i++) {
			order[i] = i;
			Node neighbor = staticl.getNeighbor(i);
			Assert.assertFalse(bsn.contains(neighbor));
			bsn.addNeighbor(neighbor);
			Assert.assertTrue(bsn.contains(neighbor));
		}
		
		assertIterationEquals(staticl, bsn, order);
		OrderingUtils.permute(0, order.length, order, fRandom);
		assertIterationEquals(staticl, bsn, order);
	}
	
	@Test
	public void testLinearMerge() {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.addNodes(10);
	
		int LINKABLE_ID = builder.assignLinkable(new long[][]{
				{0, 1, 3, 5, 6, 8, 9},
				{0, 1, 2, 3, 4, 6, 8, 9}
		});
		builder.done();
		
		BitSetNeighborhood b1 = new BitSetNeighborhood((Linkable) builder
				.getNodes().get(0).getProtocol(LINKABLE_ID));
		BitSetNeighborhood b2 = new BitSetNeighborhood((Linkable) builder
				.getNodes().get(1).getProtocol(LINKABLE_ID));
		
		b1.addNeighbor(builder.getNodes().get(1));
		b1.addNeighbor(builder.getNodes().get(3));
		b1.addNeighbor(builder.getNodes().get(5));
		b1.addNeighbor(builder.getNodes().get(9));
		
		b2.addNeighbor(builder.getNodes().get(0));
		b2.addNeighbor(builder.getNodes().get(2));
		b2.addNeighbor(builder.getNodes().get(4));
		b2.addNeighbor(builder.getNodes().get(8));

		int [] refs = {0, 1, 3, 4, 8, 9};
		int [] nrefs = {6};
	
		b2.addAll(b1);
		Assert.assertEquals(7, b2.degree());
		
		for (int ref : refs) {
			Assert.assertTrue(b2.contains(builder.getNodes().get(ref)));
		}
		
		for (int ref : nrefs) {
			Assert.assertFalse(b2.contains(builder.getNodes().get(ref)));
		}
	}
	

	private void assertIterationEquals(Linkable l1, Linkable l2, int[] order) {
		for (int idx : order) {
			Assert.assertEquals(l1.getNeighbor(idx), l2.getNeighbor(idx));
		}
	}
}
