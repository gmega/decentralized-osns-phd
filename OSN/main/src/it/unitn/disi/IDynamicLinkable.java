package it.unitn.disi;

import peersim.core.Linkable;

/**
 * Interface to a {@link Linkable} which can tell whether it has changed after
 * some given point in time or not.
 * 
 * @author giuliano
 */
public interface IDynamicLinkable extends Linkable {
	/**
	 * @param time
	 *            some time instant in simulation time.
	 *            
	 * @return <code>true</code> if this {@link Linkable} has changed after that
	 *         time instant, or <code>false</code> otherwise.
	 */
	public boolean hasChanged(int time);
}
