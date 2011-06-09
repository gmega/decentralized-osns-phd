package it.unitn.disi.newscasting.internal.demers;

import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.epidemics.IGossipMessage;

/**
 * {@link IDestinationTracker} helps {@link DemersRumorMonger} keep track of
 * which destinations it currently has potentially interesting messages to send
 * to. This has been factored out in a different object for performance reasons,
 * so we can use lighterweight implementations when circumstances allow.
 * 
 * @author giuliano
 */
interface IDestinationTracker {

	static enum Result {
		no_intersection, originator_only, forward
	}

	/**
	 * Adds a message to the tracking service.
	 * 
	 * @param message
	 *            the message to be added.
	 * @return one of:
	 *         <ol>
	 *         <li> {@link Result#no_intersection} if there is no intersection
	 *         between the {@link #constraint()} and the set of destinations for
	 *         the message. Message won't be added to the tracker in this case.</li>
	 *         <li> {@link Result#originator_only} if the only destination shared
	 *         in common is the actual originator of the message. Again, message
	 *         won't be added to the tracker in this case.</li>
	 *         <li> {@link Result#forward} if there are potential destinations.
	 *         Message will be added to the tracker in this case.</li>
	 *         </ol>
	 */
	public Result track(IGossipMessage message);

	/**
	 * Drops a message from the tracker.
	 * 
	 * @param message
	 *            the message to be dropped.
	 */
	public void drop(IGossipMessage message);

	/**
	 * Counts how many messages we have for a given destination.
	 * 
	 * @param target
	 *            the target destination.
	 * @return the number of messages for it.
	 */
	public int count(Node target);

	/**
	 * @return the contraint {@link Linkable} used for filtering.
	 */
	public Linkable constraint();

}
