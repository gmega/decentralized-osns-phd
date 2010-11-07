package it.unitn.disi.newscasting;

import it.unitn.disi.newscasting.internal.DefaultVisibility;
import it.unitn.disi.test.framework.TestNetworkBuilder;

import org.junit.Assert;
import org.junit.Test;

import peersim.core.Node;

public class TestTweet {
	@Test public void testReplyEquality() {
		TestNetworkBuilder builder = new TestNetworkBuilder();
		
		Node A = builder.baseNode();
		Node B = builder.baseNode();
		
		int pid = builder.assignCompleteLinkable();
		DefaultVisibility vis = new DefaultVisibility(pid);
		builder.replayAll();
		
		Tweet root = new Tweet(A, 1, vis);
		// Note that this is actually illegal as two different events
		// cannot have the same sequence number, but we're testing.
		Tweet reply = new Tweet(B, 1, vis, root);
		Tweet replyClone = new Tweet(B, 1, vis, root);
		Tweet noreply = new Tweet(B, 1, vis);
		
		Assert.assertTrue(reply.equals(replyClone));
		Assert.assertTrue(replyClone.equals(reply));
		Assert.assertEquals(replyClone.hashCode(), reply.hashCode());
		
		Assert.assertFalse(reply.equals(noreply));
		Assert.assertFalse(noreply.equals(reply));
		Assert.assertFalse(noreply.hashCode() == reply.hashCode());
		
		Assert.assertFalse(root.equals(reply));
		Assert.assertFalse(reply.equals(root));
		Assert.assertFalse(reply.hashCode() == root.hashCode());
	}
}
