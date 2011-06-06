package it.unitn.disi.epidemics;

import it.unitn.disi.newscasting.Tweet;
import peersim.core.Protocol;

public interface IWritableEventStorage extends IEventStorage, Protocol {
	/**
	 * Adds a {@link Tweet} to the event storage.
	 * 
	 * @return <code>true</code> if the {@link IGossipMessage} was actually
	 *         added to the event store, and <code>false</code> if it was
	 *         already in the event store.
	 */
	public boolean add(IGossipMessage tweet);

	/**
	 * Removes a {@link Tweet} from the event storage. Optional operation.
	 * 
	 * @return <code>true</code> if the event was removed, or <code>false</code>
	 *         if it were not in the event storage.
	 * @throws UnsupportedOperationException
	 *             if not supported.
	 */
	public boolean remove(IGossipMessage tweet);

	/**
	 * Removes all of the content inside of the storage.
	 */
	public void clear();
}
