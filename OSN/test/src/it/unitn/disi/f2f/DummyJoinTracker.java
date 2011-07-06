package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IGossipMessage;

import java.util.BitSet;

import junit.framework.Assert;

import peersim.core.Linkable;

public class DummyJoinTracker implements IJoinListener {
	
	private boolean fDone;
	
	private int fExpectedSteps;
	
	public DummyJoinTracker(int expectedSteps) {
	}

	@Override
	public void joinStarted(IGossipMessage message) {
		System.err.println("Join start.");
	}

	@Override
	public void descriptorsReceived(Linkable linkable, BitSet indices) {
	}

	@Override
	public boolean joinDone(IGossipMessage starting, JoinTracker tracker) {
		System.err.println("Join done.");
		Assert.assertEquals(tracker.totalTime(), fExpectedSteps);
		fDone = true;
		return true;
	}

	public void assertDone() {
		Assert.assertTrue(fDone);
	}
}
