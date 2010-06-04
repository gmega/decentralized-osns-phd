package it.unitn.disi.application;

import it.unitn.disi.TestUtils;
import it.unitn.disi.utils.IExchanger;
import it.unitn.disi.utils.OrderingUtils;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import peersim.core.Node;

public class EventStorageTest {
	@Test
	public void add() {
		final int[] a = new int[1000];

		Node node = TestUtils.makeNode();

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

		EventStorage st = new EventStorage();
		for (int i = 0; i < 1000; i++) {
			st.add(node, a[i]);
		}

		List<Integer> l = st.getList(node);
		Assert.assertEquals(0, (int) l.get(0));
		Assert.assertEquals(999, (int) l.get(1));
	}

	@Test
	public void sameSizeLists() {
		Node node = TestUtils.makeNode();
		EventStorage s1 = new EventStorage();
		EventStorage s2 = new EventStorage();

		addInterval(s1, node, 0, 9); // [0, 9]
		addInterval(s2, node, 2, 5); // [2, 5]
		addInterval(s2, node, 7, 8); // [7, 8]
		addInterval(s1, node, 11, 20); // [11, 20]
		addInterval(s2, node, 15, 25); // [15, 25]
		addInterval(s1, node, 28, 30); // [28, 30]

		s1.merge(null, null, s2, IMergeObserver.NULL, TestUtils
				.allSocialNetwork());

		this.assertIntervals(s1, node, 0, 9, 11, 25, 28, 30);
	}

	@Test
	public void testObserver() {
		Node[] array = TestUtils.mkNodeArray(10);
		final EventStorage s1 = new EventStorage();
		final EventStorage s2 = new EventStorage();

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

			final EventStorage s1Clone = (EventStorage) s1.clone();
			final EventStorage s2Clone = (EventStorage) s2.clone();

			final AtomicInteger c1 = new AtomicInteger();
			final AtomicInteger c2 = new AtomicInteger();

			final AtomicInteger o1 = new AtomicInteger();
			final AtomicInteger o2 = new AtomicInteger();

			s1.merge(null, null, s2, new IMergeObserver() {
				public void eventDelivered(Node sender, Node receiver, Node id, int start, int end) {
					for (int i = start; i <= end; i++) {
						c1.incrementAndGet();
						s1Clone.add(id, i);
					}

					o1.addAndGet(end - start + 1);
				}

				public void sendDigest(Node sender, Node receiver, Node owner,
						List<Integer> holes) { }

				public void duplicateReceived(Node sender, Node receiver,
						Node owner, int start, int end) { }

				public void tweeted(Node owner, int sequenceNumber) {
					
				}
			}, TestUtils.allSocialNetwork());

			for (Node node : array) {
				Assert.assertEquals(s1Clone.getList(node), s1.getList(node));
			}

			s2.merge(null, null, s1, new IMergeObserver() {
				public void eventDelivered(Node sender, Node receiver, Node id, int start, int end) {
					for (int i = start; i <= end; i++) {
						c2.incrementAndGet();
						s2Clone.add(id, i);
					}

					o2.addAndGet(end - start + 1);
				}
				
				public void sendDigest(Node sender, Node receiver, Node owner,
						List<Integer> holes) { }

				public void duplicateReceived(Node sender, Node receiver,
						Node owner, int start, int end) { }
				
				public void tweeted(Node owner, int sequenceNumber) {
					
				}

			}, TestUtils.allSocialNetwork());

			for (Node node : array) {
				Assert.assertEquals(s2Clone.getList(node), s2.getList(node));
			}

			Assert.assertEquals(c1.intValue(), o1.intValue());
			Assert.assertEquals(c2.intValue(), o2.intValue());
		}
	}

	private void addInterval(EventStorage storage, Node id, int start, int end) {
		for (int i = start; i <= end; i++) {
			storage.add(id, i);
		}
	}

	private void assertIntervals(EventStorage storage, Node node, int... bounds) {
		List<Integer> list = storage.getList(node);

		for (int i = 0; i < bounds.length; i++) {
			Assert.assertEquals((int) list.get(i), bounds[i]);
		}
	}
}
