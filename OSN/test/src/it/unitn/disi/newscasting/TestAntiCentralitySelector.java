package it.unitn.disi.newscasting;

import it.unitn.disi.newscasting.ISelectionFilter;
import it.unitn.disi.newscasting.internal.selectors.AntiCentralitySelector;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.MultiCounter;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import peersim.core.Linkable;
import peersim.core.Node;

public class TestAntiCentralitySelector {
	@Test public void selectPeer() throws Exception{
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.mkNodeArray(11);
		
		int pid = builder.assignLinkable(
			new long[][] {
				{1, 2, 3, 4, 5, 6, 7, 8, 9, 10},	//0
				{0, 2, 3, 4, 5, 6, 7, 8, 9, 10},	//1
				{0, 1, 3, 4, 5, 6, 7, 8, 9},		//2		
				{0, 1, 2, 4, 5, 6, 7, 8},			//3
				{0, 1, 2, 3, 5, 6, 7},				//4
				{0, 1, 2, 3, 4, 6},					//5
				{0, 1, 2, 3, 4},					//6
				{0, 1, 2, 3},						//7
				{0, 1, 2},							//8
				{0, 1},								//9
				{0}									//10
			});
		
		Random r = new Random(42);
		AntiCentralitySelector slktor = new AntiCentralitySelector(new ProtocolReference<Linkable>(pid), r);
		
		Node node = builder.getNodes().get(0);
		builder.replayAll();
		
		Linkable lnk = (Linkable) node.getProtocol(pid);
		
		MultiCounter<Node> counter = new MultiCounter<Node>();
		
		for (int i = 0; i < 20000; i++) {
			Node selected = slktor.selectPeer(node, ISelectionFilter.UP_FILTER);
			counter.increment(selected);
		}

		int last = Integer.MIN_VALUE;
		for (int i = 0; i < 10; i++) {
			Node neighbor = lnk.getNeighbor(i);
			Assert.assertEquals(i + 1, neighbor.getID());
			int selections = counter.hist(neighbor);
			Assert.assertTrue(selections > last);
			last = selections;
			System.out.print(last + " ");
		}
		
		System.out.println();
	}

}
