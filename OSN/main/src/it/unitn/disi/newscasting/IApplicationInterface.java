package it.unitn.disi.newscasting;

/**
 * {@link IApplicationInterface} defines the interface between application and
 * the social newscasting service.
 * 
 * @author giuliano
 */
public interface IApplicationInterface {

	// ----------------------------------------------------------------------
	// Communication primitives.
	// ----------------------------------------------------------------------
	
	/**
	 * Called when the application has new content to post. 
	 */
	public Tweet postToFriends();

	/**
	 * Called when the application has a reply to post, to a previously received
	 * {@link Tweet}.
	 * 
	 * @param originator
	 * @param sequenceId
	 */
	public Tweet replyToPost(Tweet original);
	
	// ----------------------------------------------------------------------
	// Accessors.
	// ----------------------------------------------------------------------
	
	/**
	 * @return a reference to the underlying {@link IEventStorage}.
	 */
	public IEventStorage storage();
}
