package it.unitn.disi.utils.tracetools;

import java.util.ArrayList;

import junit.framework.Assert;
import it.unitn.disi.utils.tracetools.ZoneCrawl2Avt;
import it.unitn.disi.utils.tracetools.ZoneCrawl2Avt.Node;

import org.junit.Test;

public class ZoneCrawl2AvtTest {

	@Test
	public void testFindsRightSpots() {
		boolean[] updown = { false, false, false, true, false, true, true,
				false, false, true, true, true };

		int[] times = { 30, 40, 50, 70, 90, 110 };
		
		Node node = new ZoneCrawl2Avt.Node("aaa", "Wonderland", 0, 10);
		
		run(node, updown, times);
	}

	@Test
	public void testHoleFilter() {
		boolean[] updown = { false, false, false, true, false, true, false,
				true, false, false, true, true, true };
		
		int[] times = {30, 80, 100, 120};
		
		Node node = new ZoneCrawl2Avt.Node("aaa", "Wonderland", 1, 10);
		
		run(node, updown, times);
	}
	
	private void run(Node node, boolean [] traces, int [] expected) {
		int i = 0;
		for (i = 0; i < traces.length; i++) {
			node.atTime(i, traces[i]);
		}
		node.done(i - 1);
		
		ArrayList<Long> intervals = node.intervals();
		for (i = 0; i < expected.length; i++) {
			Assert.assertEquals(expected[i], intervals.get(i).intValue());
		}
	}
}
