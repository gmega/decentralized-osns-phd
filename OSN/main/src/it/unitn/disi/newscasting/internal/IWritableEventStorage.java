package it.unitn.disi.newscasting.internal;

import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.Tweet;
import peersim.core.Protocol;

public interface IWritableEventStorage extends IEventStorage, Protocol {
	/**
	 * Adds a {@link Tweet} to the event storage.
	 * 
	 * @return <code>false</code> if the {@link Tweet} was already in the event
	 *         store, or <code>true</code> otherwise.
	 */
	public boolean add(Tweet tweet);
	
	/**
	 * Removes all of the content inside of the storage.
	 */
	public void clear();
}
