package it.unitn.disi.newscasting;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import peersim.core.Node;
import it.unitn.disi.ISelectionFilter;
import it.unitn.disi.newscasting.internal.selectors.CentralitySelector;
import it.unitn.disi.test.framework.TestNetworkBuilder;

public class TestCentralitySelector {
	@Test public void selectPeer() throws Exception{
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.mkNodeArray(12);
		
		builder.assignLinkable(
			new long[][] {
				{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11},//0
				{0, 2, 3, 4, 5, 6, 7, 8, 9, 10},	//1
				{0, 1, 3, 4, 5, 6, 7, 8, 9},		//2		
				{0, 1, 2, 4, 5, 6, 7, 8},			//3
				{0, 1, 2, 3, 5, 6, 7},				//4
				{0, 1, 2, 3, 4, 6},					//5
				{0, 1, 2, 3, 4},					//6
				{0, 1, 2, 3},						//7
				{0, 1, 2},							//8
				{0, 1},								//9
				{0},								//10
				{0, 1, 2, 4, 5, 6, 7}				//11
			});

		builder.replayAll();
		
		Random r = new Random(42);
		CentralitySelector slktor = new CentralitySelector(0, 0.6, r);
		Set<Integer> selectedSet = new HashSet<Integer>();
		Node node = builder.getNodes().get(0);
		

		
		for (int i = 0; i < 20000; i++) {
			Node selected = slktor.selectPeer(node, ISelectionFilter.UP_FILTER);
			selectedSet.add((int) selected.getID());
		}
		
		assertEquals(5, selectedSet.size());
		for (Integer selected : selectedSet) {
			assertTrue((selected > 0 && selected < 5) || selected == 11);
		}
	}
}
