package it.unitn.disi.epidemics;

/**
 * Evolving interface which allows rumor mongering protocols to communicate
 * relevant events to clients.
 * 
 * @author giuliano
 */
public interface IRMEventObserver {
	/**
	 * Called when the rumor mongering protocol decides to stop sending a
	 * message.
	 */
	public void dropped(IGossipMessage message);
}
