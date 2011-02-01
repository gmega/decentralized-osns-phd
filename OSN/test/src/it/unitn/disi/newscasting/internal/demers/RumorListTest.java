package it.unitn.disi.newscasting.internal.demers;

import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.DefaultVisibility;
import it.unitn.disi.test.framework.TestNetworkBuilder;

import java.util.ArrayList;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import peersim.core.Linkable;
import peersim.core.Node;

public class RumorListTest {

	TestNetworkBuilder builder;

	@Before
	public void initialize() {
		builder = new TestNetworkBuilder();
	}

	@Test
	public void testNonFullList() {
		RumorList rList = new RumorList(10, 0.0, new Random());

		Node node = builder.baseNode();
		int pid = builder.assignCompleteLinkable();
		builder.done();

		for (int i = 0; i < 8; i++) {
			rList.add((Linkable) node.getProtocol(pid), new Tweet(node, i,
					new DefaultVisibility(pid)));
		}

		ArrayList<Boolean> demotion = new ArrayList<Boolean>();
		demotion.add(false);
		demotion.add(false);
		demotion.add(false);
		demotion.add(true);

		// Some garbage.
		demotion.add(true);
		demotion.add(true);
		demotion.add(true);
		demotion.add(true);

		int[] expected = new int[] { 0, 1, 2, 4, 5, 6, 3, 7 };

		rList.demote(demotion, 4);

		for (int i = 0; i < rList.size(); i++) {
			Assert.assertEquals(expected[i],
					rList.getList().get(i).sequenceNumber);
		}
	}

	@SuppressWarnings("serial")
	@Test
	public void testFullList() {
		RumorList rList = new RumorList(10, 0.5, new Random() {
			int i = 0;

			@Override
			public double nextDouble() {
				double val;
				if (i == 3 || i == 6) {
					val = 0.4;
				} else {
					val = 0.8;
				}
				i++;
				return val;
			}
		});

		Node node = builder.baseNode();
		int pid = builder.assignCompleteLinkable();
		builder.done();

		for (int i = 0; i < 19; i++) {
			rList.add((Linkable) node.getProtocol(pid), new Tweet(node, i,
					new DefaultVisibility(pid)));
		}

		for (int i = 0; i < rList.getList().size(); i++) {
			Assert.assertEquals(9 + i, rList.getList().get(i).sequenceNumber);
		}

		ArrayList<Boolean> demotion = new ArrayList<Boolean>();
		demotion.add(false);
		demotion.add(false);
		demotion.add(false);
		demotion.add(true);
		demotion.add(false);
		demotion.add(true);
		demotion.add(false);
		demotion.add(false);
		demotion.add(true);
		demotion.add(false);

		int[] expected = new int[] { 10, 11, 9, 12, 15, 16, 14, 17 };
		rList.demote(demotion, rList.getList().size());
		for (int i = 0; i < rList.size(); i++) {
			Assert.assertEquals(expected[i],
					rList.getList().get(i).sequenceNumber);
		}
	}

}
