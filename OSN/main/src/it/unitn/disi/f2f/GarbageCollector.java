package it.unitn.disi.f2f;

import it.unitn.disi.epidemics.IGossipMessage;
import it.unitn.disi.newscasting.internal.IWritableEventStorage;
import it.unitn.disi.utils.IReference;

public class GarbageCollector implements IJoinListener {

	private final IReference<IWritableEventStorage> fStorage;

	public GarbageCollector(IReference<IWritableEventStorage> ref) {
		fStorage = ref;
	}

	@Override
	public void joinStarted(IGossipMessage message) {
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
