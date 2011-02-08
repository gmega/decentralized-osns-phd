package it.unitn.disi.newscasting.experiments;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import junit.framework.Assert;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.newscasting.ComponentComputationService;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.peersim.FallThroughReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import org.junit.Test;

import peersim.core.Node;

public class ClusteringRankingTest extends PeerSimTest{
	@Test
	public void rankByClustering() {
		long[][] ids = { 
				{ 1, 2, 3, 4, 5, 6, 7 }, 
				{ 0 }, 
				{ 0 }, 
				{ 0, 4 },
				{ 0, 3 }, 
				{ 0, 6 }, 
				{ 0, 5, 7 }, 
				{ 0, 6 } 
			};
		
		int [][] ref = {
				{1},
				{2},
				{5, 6, 7},
				{3, 4}
		};
		
		TestNetworkBuilder tnb = new TestNetworkBuilder();
		tnb.addNodes(8);
		final Node root = tnb.getNodes().get(0);
		int linkable = tnb.assignLinkable(ids);
		tnb.done();

		ComponentComputationService css = new ComponentComputationService(
				linkable);
		css.initialize(root);
		final ClusteringRanking cr = new ClusteringRanking(
				new FallThroughReference<ComponentComputationService>(css),
				new ProtocolReference<GraphProtocol>(linkable), false);
		
		Integer [] idx = new Integer[ref.length];
		for (Integer i = 0; i < idx.length; i++) {
			idx[i] = i;
		}
		
		Arrays.sort(idx, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return cr.utility(root, o1) - cr.utility(root, o2);
			}
		});
		
		for (int i = 0; i < idx.length; i++) {
			assertEquals(ref[i], (Integer[]) css.members(idx[i]).toArray(new Integer[0]));
		}
	}

	private void assertEquals(int[] l1, Integer[] l2) {
		Arrays.sort(l1);
		Arrays.sort(l2);
		Assert.assertEquals(l1.length, l2.length);
		for (int i = 0; i < l1.length; i++) {
			Assert.assertEquals(l1[i], l2[i].intValue());
		}
	}
}
