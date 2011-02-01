package it.unitn.disi.newscasting;

import it.unitn.disi.newscasting.internal.CompactEventStorage;
import it.unitn.disi.newscasting.internal.DefaultVisibility;
import it.unitn.disi.newscasting.internal.IMergeObserver;
import it.unitn.disi.test.framework.PeerSimTest;
import it.unitn.disi.test.framework.TestNetworkBuilder;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.collections.IExchanger;
import it.unitn.disi.utils.peersim.SNNode;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import peersim.core.Linkable;
import peersim.core.Node;

public class EventStorageTest extends PeerSimTest{
	
	TestNetworkBuilder builder;
	
	@Before
	public void initialize () {
		builder = new TestNetworkBuilder();
	}
	
	@Test
	public void add() {
		final int[] a = new int[1000];
		
		Node node = builder.baseNode();
		int pid = builder.assignCompleteLinkable();
		DefaultVisibility vis = new DefaultVisibility(pid);
		
		builder.done();

		for (int i = 0; i < 1000; i++) {
			a[i] = i;
		}

		OrderingUtils.permute(0, 1000, new IExchanger() {
			public void exchange(int i, int j) {
				int tmp;
				tmp = a[i];
				a[i] = a[j];
				a[j] = tmp;
			}
		}, new Random());

		CompactEventStorage st = new CompactEventStorage(vis);
		for (int i = 0; i < 1000; i++) {
			st.add(node, a[i]);
		}

		List<Integer> l = st.eventsFor(node);
		Assert.assertEquals(0, (int) l.get(0));
		Assert.assertEquals(999, (int) l.get(1));
	}

	@Test
	public void sameSizeLists() {
		Node node = builder.baseNode();
		int pid = builder.assignCompleteLinkable();
		DefaultVisibility vis = new DefaultVisibility(pid);
		
		CompactEventStorage s1 = new CompactEventStorage(vis);
		CompactEventStorage s2 = new CompactEventStorage(vis);

		addInterval(s1, node, 0, 9); // [0, 9]
		addInterval(s2, node, 2, 5); // [2, 5]
		addInterval(s2, node, 7, 8); // [7, 8]
		addInterval(s1, node, 11, 20); // [11, 20]
		addInterval(s2, node, 15, 25); // [15, 25]
		addInterval(s1, node, 28, 30); // [28, 30]
		
		builder.done();
		Linkable sn = (Linkable) node.getProtocol(pid);

		s1.merge(null, null, s2, IMergeObserver.NULL, sn);

		this.assertIntervals(s1, node, 0, 9, 11, 25, 28, 30);
	}

	@Test
	public void testObserver() {
		Node[] array = builder.addNodes(10);
		int pid = builder.assignCompleteLinkable();
		DefaultVisibility vis = new DefaultVisibility(pid);

		final CompactEventStorage s1 = new CompactEventStorage(vis);
		final CompactEventStorage s2 = new CompactEventStorage(vis);
		
		builder.done();
		Linkable sn = (Linkable) array[0].getProtocol(pid);

		Random rnd = new Random(1);

		for (int k = 1; k <= 500; k++) {
			for (int j = 0; j < 10; j++) {
				for (int i = 0; i <= 40; i++) {
					int start = rnd.nextInt(k);
					if (rnd.nextBoolean()) {
						addInterval(s1, array[j], start, start + 1);
					}

					start = rnd.nextInt(k);
					if (rnd.nextBoolean()) {
						addInterval(s2, array[j], start, start + 1);
					}
				}
			}

			final CompactEventStorage s1Clone = (CompactEventStorage) s1.clone();
			final CompactEventStorage s2Clone = (CompactEventStorage) s2.clone();

			final AtomicInteger c1 = new AtomicInteger();
			final AtomicInteger c2 = new AtomicInteger();

			final AtomicInteger o1 = new AtomicInteger();
			final AtomicInteger o2 = new AtomicInteger();
			
			s1.merge(null, null, s2, new IMergeObserver() {
				public void eventDelivered(SNNode sender, SNNode receiver,
						Tweet tweet, boolean duplicate) {
					c1.incrementAndGet();
					s1Clone.add(tweet.poster, tweet.sequenceNumber);
					o1.addAndGet(1);
				}

				public void sendDigest(Node sender, Node receiver, Node owner,
						List<Integer> holes) { }

				public void tweeted(Tweet t) { }
			}, sn);

			for (Node node : array) {
				Assert.assertEquals(s1Clone.eventsFor(node), s1.eventsFor(node));
			}

			s2.merge(null, null, s1, new IMergeObserver() {
				public void eventDelivered(SNNode sender, SNNode receiver,
						Tweet tweet, boolean duplicate) {
					c2.incrementAndGet();
					s2Clone.add(tweet.poster, tweet.sequenceNumber);

					o2.addAndGet(1);
				}
				
				public void sendDigest(Node sender, Node receiver, Node owner,
						List<Integer> holes) { }

				public void tweeted(Tweet t) {
					
				}

			}, sn);

			for (Node node : array) {
				Assert.assertEquals(s2Clone.eventsFor(node), s2.eventsFor(node));
			}

			Assert.assertEquals(c1.intValue(), o1.intValue());
			Assert.assertEquals(c2.intValue(), o2.intValue());
		}
	}

	private void addInterval(CompactEventStorage storage, Node id, int start, int end) {
		for (int i = start; i <= end; i++) {
			storage.add(id, i);
		}
	}

	private void assertIntervals(CompactEventStorage storage, Node node, int... bounds) {
		List<Integer> list = storage.eventsFor(node);

		for (int i = 0; i < bounds.length; i++) {
			Assert.assertEquals((int) list.get(i), bounds[i]);
		}
	}
}
