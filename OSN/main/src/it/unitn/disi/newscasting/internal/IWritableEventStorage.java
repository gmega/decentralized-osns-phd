package it.unitn.disi.newscasting.internal;

import peersim.core.Protocol;
import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.Tweet;

public interface IWritableEventStorage extends IEventStorage, Protocol {
	/**
	 * Adds a {@link Tweet} to the event storage.
	 * 
	 * @return <code>false</code> if the {@link Tweet} was already in the event
	 *         store, or <code>true</code> otherwise.
	 */
	public boolean add(Tweet tweet);
}
