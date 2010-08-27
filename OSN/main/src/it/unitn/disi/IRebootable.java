package it.unitn.disi;

/**
 * Generic interface to objects which can be rebooted. Originally intended to be
 * called by node initializers when re-initializing nodes which were previously
 * down.
 * 
 * @author giuliano
 */
public interface IRebootable {
	/**
	 * Clears some piece of internal state.
	 */
	public void reset();
}
