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
	 *            the {@link Message} to post.
	 * 
	 * @param state
	 *            a simulation engine.
	 */
	public void post(Message message, ISimulationEngine engine);

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
