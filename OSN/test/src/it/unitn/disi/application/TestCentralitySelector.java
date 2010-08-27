package it.unitn.disi.application;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import peersim.core.Node;
import it.unitn.disi.TestLinkable;
import it.unitn.disi.newscasting.ISelectionFilter;
import it.unitn.disi.newscasting.internal.selectors.CentralitySelector;

public class TestCentralitySelector {
	@Test public void selectPeer() throws Exception{
		TestLinkable lnk = TestLinkable.testLinkable(
			new int[][] {
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
			}, 0);
		
		Random r = new Random(42);
		CentralitySelector slktor = new CentralitySelector(0, 0.6, 0, r);
		Node node = lnk.get(0);
		Set<Integer> selectedSet = new HashSet<Integer>();
		
		lnk.replayAll();
		
		for (int i = 0; i < 20000; i++) {
			Node selected = slktor.selectPeer(node, ISelectionFilter.UP_FILTER);
			selectedSet.add(lnk.indexOf(selected));
		}
		
		assertEquals(4, selectedSet.size());
		for (Integer selected : selectedSet) {
			assertTrue(selected > 0 && selected < 5);
		}
	}
}
