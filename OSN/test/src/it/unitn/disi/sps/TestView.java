package it.unitn.disi.sps;

import static org.junit.Assert.*;
import it.unitn.disi.sps.SocialPeerSampling;
import it.unitn.disi.sps.View;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.test.framework.TestUtils;
import it.unitn.disi.utils.IExchanger;
import it.unitn.disi.utils.OrderingUtils;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import junit.framework.TestCase;
import peersim.core.Linkable;
import peersim.core.Node;

public class TestView {

	Random r = new Random(42);

	SocialPeerSampling p1;
	SocialPeerSampling p2;

	int[] tStamps1;
	int[] tStamps2;
	
	TestNetworkBuilder builder;

	Linkable sn = TestUtils.completeSocialNetwork();
	
	@Before
	public void preInit(){
		builder = new TestNetworkBuilder();
	}

	public void init() {
		p1 = new SocialPeerSampling(10, 2, 2, r, false, false);
		p2 = new SocialPeerSampling(10, 2, 2, r, false, false);

		Node[] nd1 = builder.mkNodeArray(10);
		Node[] nd2 = builder.mkNodeArray(10);

		for (int i = 0; i < 10; i++) {
			p1.view().append(nd1[i], tStamps1[i]);
			p2.view().append(nd2[i], tStamps2[i]);
		}

	}

	@Test
	public void load() throws Exception {
		View v = new View(10, r);
		ArrayList<Node> nodes = new ArrayList<Node>();
		for (int i = 0; i < 10; i++) {
			Node node = builder.baseNode();
			nodes.add(node);
			v.set(i, node, i);
		}

		View.beginExchange();

		v.buffer(0).load();

		for (int i = 0; i < 10; i++) {
			assertEquals(nodes.get(i), v.buffer(0).getNode(i));
			assertEquals(i, v.buffer(0).getTimestamp(i));
		}
	}

	@Test
	public void appendBuffer() {

		// 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14
		int[] ts1 = { 5, 1, 3, 4, 7, 8, 12, 11, 25, 20, 12, 13, 14, 1, 0 };
		int[] ts2 = { 30, 33, 25, 12, 1 };

		int[] afterMergeTs = { 1, 3, 4, 7, 8, 12, 11, 25, 20, 12, 13, 0, // from
				// ts1
				0, // freshly added
				30, 12, 1 // ts2
		};

		Node[] nd1 = builder.mkNodeArray(15);
		Node[] nd2 = { builder.baseNode(), // Should be added to the buffer.
				nd1[1], // Should not be added to the buffer.
				nd1[8], // Should not be added to the buffer.
				nd1[12], // Should replace nd1[12] in the buffer.
				nd1[0], // Should replace nd[0] in the buffer.
		};

		Node receiver = nd1[13];

		ArrayList<Node> nodes = new ArrayList<Node>();
		appendNodes(nd1, nodes, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 14);
		nodes.add(receiver);
		appendNodes(nd2, nodes, 0, 3, 4);

		View v1 = viewFrom(ts1, nd1, 20);
		View v2 = viewFrom(ts2, nd2, 20);

		View.beginExchange();

		v1.buffer(View.SENDER).load();
		v2.buffer(View.SENDER).appendHead(receiver, sn);
		v1.buffer(View.SENDER).store();

		// Two elements added, two replaced.
		assertEquals(16, v1.size());

		for (int i = 0; i < v1.size(); i++) {
			assertEquals(nodes.get(i), v1.buffer(View.SENDER).getNode(i));
			assertEquals(afterMergeTs[i], v1.buffer(View.SENDER)
					.getTimestamp(i));
		}
	}

	@Test
	public void pivot() {
		int SIZE = 20000;

		final int[] data = new int[SIZE];
		IExchanger xchg = new IExchanger() {
			public void exchange(int i, int j) {
				int tmp = data[i];
				data[i] = data[j];
				data[j] = tmp;
			}
		};

		for (int i = 0; i < SIZE; i++) {
			data[i] = i;
		}

		OrderingUtils.permute(0, SIZE, xchg, r);

		int pivValue = data[SIZE / 2];
		int finalPos = pivValue;
		OrderingUtils.partition(0, SIZE - 1, SIZE / 2, data, xchg);

		assertEquals(pivValue, data[finalPos]);

		for (int i = 0; i < SIZE; i++) {
			if (i < finalPos) {
				assertTrue(data[i] < pivValue);
			} else if (i > finalPos) {
				assertTrue(data[i] > pivValue);
			}
		}
	}

	@Test
	public void removeOldest() {
		Node rec = builder.baseNode();

		tStamps1 = new int[] { 5, 23, 12, 2, 4, 9, 10, 3, 1, 2 };
		tStamps2 = new int[] { 13, 4, 2, 7, 9, 1, 7, 1, 1, 4 };
		init();
		View.beginExchange();
		p1.view().buffer(View.SENDER).load();
		p2.view().buffer(View.SENDER).appendHead(rec, sn);
		int result = p1.view().buffer(View.SENDER).removeOldest(4);
		assertEquals(10, result);
	}

	@Test public void kLargest() {
		final int[] data = new int[4000];
		IExchanger xchg = new IExchanger() {
			public void exchange(int i, int j) {
				int tmp = data[i];
				data[i] = data[j];
				data[j] = tmp;
			}
		};

		for (int i = 0; i < 2000; i++) {
			data[i] = i;
		}

		OrderingUtils.permute(0, 2000, xchg, r);
		OrderingUtils.orderByKthLargest(249, 0, 1999, data, xchg, r);
		assertEquals(249, data[249]);
	}

	@Test
	public void fullCycle() {
		Random r = new Random(42);

		View.beginExchange();

		SocialPeerSampling p1 = new SocialPeerSampling(10, 2, 2, r,	false, false);
		SocialPeerSampling p2 = new SocialPeerSampling(10, 2, 2, r,	false, false);

		int[] tStamps1 = { 5, 7, 1, 2, 4, 9, 10, 3, 1, 2 };
		int[] tStamps2 = { 13, 4, 2, 7, 9, 1, 23, 12, 1, 4 };

		Node[] nd1 = builder.mkNodeArray(10);
		Node[] nd2 = builder.mkNodeArray(10);

		for (int i = 0; i < 10; i++) {
			p1.view().append(nd1[i], tStamps1[i]);
			p2.view().append(nd2[i], tStamps2[i]);
		}

		p1.doCycle(p2, sn, sn, builder.baseNode(), builder.baseNode());
	}

	private View viewFrom(int[] ts, Node[] nd, int capacity) {
		View v = new View(capacity, r);
		for (int i = 0; i < ts.length; i++) {
			v.set(i, nd[i], ts[i]);
		}
		return v;
	}

	public void appendNodes(Node[] array, ArrayList<Node> store, int... nodes) {
		for (int node : nodes) {
			store.add(array[node]);
		}
	}
}
