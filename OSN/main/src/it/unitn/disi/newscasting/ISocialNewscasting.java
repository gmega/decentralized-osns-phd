package it.unitn.disi.newscasting;

import it.unitn.disi.epidemics.IApplicationInterface;

/**
 * {@link ISocialNewscasting} defines the interface between application and the
 * social newscasting service.
 * 
 * XXX this is slightly different from what we put in the paper, should change
 * it here for consistency.
 * 
 * @author giuliano
 */
public interface ISocialNewscasting extends IApplicationInterface {
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
}
