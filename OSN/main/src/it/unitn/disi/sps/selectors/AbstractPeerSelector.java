package it.unitn.disi.sps.selectors;

import it.unitn.disi.sps.IView;

public abstract class AbstractPeerSelector implements ISelector {
	
	protected IView fView;
	
	public AbstractPeerSelector(IView view) {
		fView = view;
	}
	
	/**
	 * Sets the {@link IView} object on which to operate on.
	 * 
	 * @param view
	 *            the {@link IView} instance from which to select from.
	 */
	public void setView(IView view) {
		fView = view;
	}
}
