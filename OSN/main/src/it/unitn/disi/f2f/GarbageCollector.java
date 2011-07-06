package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.utils.IReference;

import java.util.BitSet;

import peersim.core.Linkable;
import peersim.core.Node;

/**
 * {@link GarbageCollector} removes messages from the underlying storage when
 * the join process is known to be over.
 * 
 * @author giuliano
 */
public class GarbageCollector implements IJoinListener {

	private final IReference<IWritableEventStorage> fStorage;

	public GarbageCollector(IReference<IWritableEventStorage> ref) {
		fStorage = ref;
	}

	@Override
	public void joinStarted(IGossipMessage message) {
	}

	@Override
	public void descriptorsReceived(Linkable linkable, BitSet indices) {
	}

	@Override
	public boolean joinDone(IGossipMessage starting, JoinTracker tracker) {
		int destinations = starting.destinations();
		int expected = tracker.propagators();
		expected = remove(starting, starting.originator(), expected);
		for (int i = 0; i < destinations; i++) {
			expected = remove(starting, starting.destination(i), expected);
		}

		if (expected != 0) {
			throw new IllegalStateException("Garbage collection failed ("
					+ expected + ").");
		}

		return false;
	}

	private int remove(IGossipMessage starting, Node node, int expected) {
		IWritableEventStorage storage = fStorage.get(node);
		return storage.remove(starting.originator(), starting.sequenceNumber()) ? expected - 1
				: expected;
	}

}
