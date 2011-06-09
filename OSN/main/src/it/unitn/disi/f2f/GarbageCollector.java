package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.epidemics.IWritableEventStorage;
import it.unitn.disi.utils.IReference;

import java.util.BitSet;

import peersim.core.Linkable;

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
	public void joinDone(IGossipMessage message, int copies) {
		int destinations = message.destinations();
		int expected = copies;
		for (int i = 0; i < destinations; i++) {
			IWritableEventStorage storage = fStorage
					.get(message.destination(i));
			if (storage.remove(message)) {
				expected--;
			}
		}
		if (expected != 0) {
			throw new IllegalStateException(
					"Garbage collector could not locate all message copies.");
		}
	}

}
