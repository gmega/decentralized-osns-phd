package it.unitn.disi.util;

import it.unitn.disi.newscasting.experiments.schedulers.IScheduleIterator;
import it.unitn.disi.newscasting.experiments.schedulers.IntervalScheduler;

import junit.framework.Assert;

import org.junit.Test;

public class SequentialSchedulerTest {
	@Test
	public void testScheduler() {
		String intervals = "0,5 6,7 8,8 9,20";

		int[][] expected = new int[][] { { 0, 1, 2, 3, 4, 5 }, { 6, 7 }, { 8 },
				{ 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 } };

		IntervalScheduler scheduler = new IntervalScheduler(intervals);
		IScheduleIterator it = scheduler.iterator();

		for (int i = 0; i < expected.length; i++) {
			for (int j = 0; j < expected[i].length; j++) {
				Assert.assertEquals(expected[i][j],
						((Integer) it.nextIfAvailable()).intValue());
			}
		}

		Assert.assertEquals(IScheduleIterator.DONE,
				((Integer) it.nextIfAvailable()).intValue());
		Assert.assertEquals(IScheduleIterator.DONE,
				((Integer) it.nextIfAvailable()).intValue());
	}
}
