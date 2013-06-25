package it.unitn.disi.churn.diffusion;

import junit.framework.Assert;

import org.junit.Test;

import peersim.util.IncrementalStats;

public class BandwidthTrackerTest {

	@Test
	public void testBandwidthTracker() {
		testWithBase(0, 1.0 / 3600);
		testWithBase(50, 1.0 / 3600);
		testWithBase(150, 1.0 / 3600);
		testWithBase(250, 1.0 / 3600);
	}
	
	public void testWithBase(double base, double width) {
		BandwidthTracker tracker = new BandwidthTracker(width);

		// 3
		tracker.messageReceived(base + 0.1 * width);
		tracker.messageReceived(base + 0.2 * width);
		tracker.messageReceived(base + 0.9 * width);
		
		// 3
		tracker.messageReceived(base + 1.0 * width);
		tracker.messageReceived(base + 1.0 * width);
		tracker.messageReceived(base + 1.1 * width);
		
		// 1
		tracker.messageReceived(base + 2.3 * width);
		
		// 0 0 1
		tracker.messageReceived(base + 5 * width);
		tracker.messageReceived(base + 5 * width);
		
		// Add something just to flush the bin.
		tracker.truncate();
		
		IncrementalStats stats = tracker.getStats();
		
		Assert.assertEquals(3.0, stats.getMax());
		Assert.assertEquals(0.0, stats.getMin());
		
		Assert.assertEquals(9/6.0, stats.getAverage());
	}

}
