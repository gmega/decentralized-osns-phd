package it.unitn.disi.application;

import it.unitn.disi.TestUtils;
import it.unitn.disi.newscasting.Tweet;

import org.junit.Assert;
import org.junit.Test;

import peersim.core.Node;

public class TestTweet {
	@Test public void testReplyEquality() {
		Node A = TestUtils.makeNode();
		Node B = TestUtils.makeNode();
		
		Tweet root = new Tweet(A, 1);
		// Note that this is actually illegal as two different events
		// cannot have the same sequence number, but we're testing.
		Tweet reply = new Tweet(B, 1, root);
		Tweet replyClone = new Tweet(B, 1, root);
		Tweet noreply = new Tweet(B, 1);
		
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
