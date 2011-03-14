package it.unitn.disi.utils.peersim;

/**
 * Observer interface for getting information about the changes in
 * {@link SNNode#getFailState()}.
 * 
 * @author giuliano
 */
public interface INodeStateListener {
	/**
	 * Informs clients about a delta in {@link SNNode#getFailState()}.
	 * 
	 * @param node
	 *            the {@link SNNode} which is changing state.
	 * @param oldState
	 *            state before the current delta.
	 * @param newState
	 *            state after the current delta.
	 */
	public void stateChanged(int oldState, int newState, SNNode node);
}
