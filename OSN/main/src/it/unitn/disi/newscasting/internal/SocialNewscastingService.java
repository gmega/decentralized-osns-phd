package it.unitn.disi.newscasting.internal;

import it.unitn.disi.epidemics.NeighborhoodMulticast;
import it.unitn.disi.epidemics.ProtocolRunner;
import it.unitn.disi.newscasting.IMessageVisibility;
import it.unitn.disi.newscasting.ISocialNewscasting;
import it.unitn.disi.newscasting.Tweet;

import java.io.IOException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Linkable;

/**
 * Base implementation for the social newscasting service.
 * 
 * @author giuliano
 */
@AutoConfig
public class SocialNewscastingService extends ProtocolRunner implements
		ISocialNewscasting {

	// ----------------------------------------------------------------------
	// Parameters.
	// ----------------------------------------------------------------------

	/**
	 * ID of the {@link Linkable} containing the social neighborhood.
	 */
	private int fSocialNetworkID;

	/** Tweet sequence number. */
	private int fSeqNumber = 0;

	// ----------------------------------------------------------------------
	// Misc.
	// ----------------------------------------------------------------------

	private IMessageVisibility fVisibility;

	// ----------------------------------------------------------------------

	public SocialNewscastingService(
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("social_neighborhood") int socialNetworkId)
			throws IOException {
		super(resolver, prefix);
		init(socialNetworkId);
	}
	
	// ----------------------------------------------------------------------

	public SocialNewscastingService(String prefix, int protocolID,
			int socialNetworkId, IApplicationConfigurator configurator) {
		super(prefix, protocolID, configurator);
		init(socialNetworkId);
	}
	
	// ----------------------------------------------------------------------
	
	private void init(int socialNetworkId) {
		fSocialNetworkID = socialNetworkId;
		fVisibility = new ProfilePageMulticast(fSocialNetworkID);
	}

	// ----------------------------------------------------------------------

	public Tweet postToFriends() {
		return this.replyToPost(null);
	}

	// ----------------------------------------------------------------------

	public Tweet replyToPost(Tweet replyTo) {
		// Sanity check.
		if (replyTo != null && !storage().contains(replyTo)) {
			throw new IllegalArgumentException(
					"Can't reply to an unknown post.");
		}

		// As per the contract.
		Tweet tweet = new Tweet(node(), ++fSeqNumber, fVisibility, replyTo);

		// Performs local delivery.
		fObserver.localDelivered(tweet);

		return tweet;
	}

	// ----------------------------------------------------------------------
	
	public int socialNetworkId() {
		return fSocialNetworkID;
	}
}

