package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.ISimulationEngine;

/**
 * {@link IDisseminationService} allows clients to post messages for
 * dissemination.
 * 
 * @author giuliano
 */
public interface IDisseminationService {

	/**
	 * Post a new message.
	 * 
	 * @param message
	 *            the {@link HFloodMMsg} to post.
	 * 
	 * @param state
	 *            a simulation engine.
	 */
	public void post(IMessage message, ISimulationEngine engine);

	/**
	 * @return the number of the sequence number of the latest update known by
	 *         this {@link IDisseminationService}.
	 */
	public int latestSequence();

	/**
	 * Registers an {@link IMessageObserver} with this dissemination service.
	 */
	public void addMessageObserver(IMessageObserver observer);

	/**
	 * Removes a previously registered {@link IMessageObserver} from this
	 * dissemination service.
	 */
	public void removeMessageObserver(IMessageObserver observer);

}
