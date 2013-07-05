package it.unitn.disi.churn.diffusion;

import java.util.Random;

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

	@Test
	public void testSequentialLarge() {
		double total_time = 10000;
		double second = 1.0 / 3600.0;

		BandwidthTrackerStats tracker = new BandwidthTrackerStats(second);
		Random rnd = new Random();

		for (double time = 0; time < total_time; time += rnd.nextDouble()
				* second) {
			tracker.at(time).messageReceived();
		}

		tracker.at(10000).end();

		int buckets = tracker.getStats().getN();
		Assert.assertEquals(buckets, (int) (total_time / second));
	}

	@Test
	public void testRoundoffAnomaly() throws Exception {
		BandwidthTrackerStats tracker1 = new BandwidthTrackerStats(0.0,
				1.0 / 3600);
		BandwidthTrackerStats tracker2 = new BandwidthTrackerStats(0.0,
				1.0 / 3600);

		double total = 0.0;

		Random rnd = new Random(42);
		for (int i = 0; i < 50000; i++) {
			double inc = rnd.nextDouble() * 10;
			total += inc;
			if (i % 2 == 0) {
				tracker2.at(total).messageReceived();
			} else {
				tracker1.at(total).messageReceived();
			}

			if (diverges(tracker2, total) || diverges(tracker1, total)) {
				Assert.fail("FAIL AT " + i);
			}

		}

		tracker1.at(total).end();
		tracker2.at(total).end();

		int expectedBuckets = (int) Math.ceil(total * 3600);
		Assert.assertEquals(expectedBuckets, tracker2.getStats().getN());
		Assert.assertEquals(expectedBuckets, tracker1.getStats().getN());
	}

	
	@Test
	public void testRoundoffAnomaly2() throws Exception {
		double START = 62.042009572028704D;
		double TRUNCATE = 74.4072956693957D;
		
		double [] schedule = { 
			63.845555555555556D,
			64.1192857753886D,
			65.08878871580927D,
			72.98349743741272D,
			74.4072956693957D
		};
		
		BandwidthTrackerStats tracker = new BandwidthTrackerStats(START,
				1.0 / 3600.0);

		for (int i = 0; i < schedule.length; i++) {
			tracker.at(schedule[i]).messageReceived();
		}
		// Should not throw exception.
		tracker.at(TRUNCATE).end();
	}

	private boolean diverges(BandwidthTrackerStats tracker, double total)
			throws Exception {
		int expectedBuckets = (int) Math.ceil(total * 3600);
		BandwidthTrackerStats stats = (BandwidthTrackerStats) tracker.clone();
		stats.at(total).end();

		if (stats.getStats().getN() != expectedBuckets) {
			System.err.println(stats.getStats().getN() + " != "
					+ expectedBuckets);
			return true;
		}

		return false;
	}

	public void testWithBase(double base, double width) {
		BandwidthTrackerStats tracker = new BandwidthTrackerStats(base, width);

		// 3
		tracker.at(base).messageReceived();
		tracker.at(base + 0.2 * width).messageReceived();
		tracker.at(base + 0.9 * width).messageReceived();

		// 3
		tracker.at(base + 1.0 * width).messageReceived();
		tracker.at(base + 1.0 * width).messageReceived();
		tracker.at(base + 1.1 * width).messageReceived();

		// 1
		tracker.at(base + 2.3 * width).messageReceived();

		// 0 0 1
		tracker.at(base + 5.1 * width).messageReceived();
		tracker.at(base + 5.1 * width).messageReceived();

		// Add something just to flush the bin.
		tracker.end();

		IncrementalStats stats = tracker.getStats();

		Assert.assertEquals(3.0, stats.getMax());
		Assert.assertEquals(0.0, stats.getMin());

		Assert.assertEquals(9 / 6.0, stats.getAverage());
	}

}
