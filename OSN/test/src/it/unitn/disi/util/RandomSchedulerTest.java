package it.unitn.disi.util;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import peersim.core.OverlayGraph;
import peersim.graph.Graph;

import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;

public class RandomSchedulerTest extends PeerSimTest{
	
	@Test
	public void testRandomScheduler() throws Exception { 
		
		TestNetworkBuilder builder = new TestNetworkBuilder();
		builder.mkNodeArray(21);
		
		int pid = builder.assignLinkable(
			new long[][] {
				{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20},	//0
				{0, 2, 3, 4, 5, 6, 7, 8, 9, 10},	//1
				{0, 1, 3, 4, 5, 6, 7, 8, 9},		//2		
				{0, 1, 2, 4, 5, 6, 7, 8},			//3
				{0, 1, 2, 3, 5, 6, 7},				//4
				{0, 1, 2, 3, 5, 6, 7},				//5
				{0, 1, 2, 3, 5, 6, 7},				//6
				{0, 1, 2, 3, 4, 6},					//7
				{0, 1, 2, 3, 4},					//8
				{0, 1, 2, 3},						//9
				{0, 1, 2},							//10
				{0, 1, 2},							//11
				{0, 1},								//12
				{0, 2},								//13
				{0, 3},								//14
				{0, 4},								//15
				{0, 5},								//16
				{0, 6},								//17
				{0, 7},								//18
				{0, 8},								//19
				{0},								//20
			});
		
		builder.replayAll();
		
		Graph graph = new OverlayGraph(pid);
		
		Set<Integer> selected = new HashSet<Integer>();
		Set<Integer> degreeClasses = new HashSet<Integer>();
		int [] classes = new int [] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20};
			
		for (int i = 0; i < 200; i++) {
			DegreeClassScheduler scheduler = new DegreeClassScheduler(pid, 14, "42");
			scheduler.initialize(graph);
			for (int klass : classes) { degreeClasses.add(klass); }
			selected.clear();
			
			for (Integer integer : scheduler) {
				Assert.assertFalse(integer.toString(), selected.contains(integer));
				selected.add(integer);
				degreeClasses.remove(graph.degree(integer));
			}
			
			Assert.assertEquals(14, selected.size());
			
			Assert.assertTrue(degreeClasses.toString(), degreeClasses.isEmpty());
		}
	}
}