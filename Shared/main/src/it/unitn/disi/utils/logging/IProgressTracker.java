package it.unitn.disi.utils.logging;

/**
 * {@link IProgressTracker} allows progress information about a computation to
 * be tracked and displayed.
 * 
 * @author giuliano
 */
public interface IProgressTracker {

	/**
	 * Called by the computation when it's about to start.
	 */
	public abstract void startTask();

	/**
	 * Convenience method, called when unit of work is done. Equivalent to
	 * <code>ticks(1)</code>.
	 */
	public abstract void tick();

	/**
	 * Called when multiple units of work are done.
	 * 
	 * @param ticks
	 */
	public abstract void tick(int ticks);

	/**
	 * Called when the entire computation is done.
	 */
	public abstract void done();

	/**
	 * @return the title for this {@link IProgressTracker}.
	 */
	public abstract String title();

}