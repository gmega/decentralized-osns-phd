package it.unitn.disi.application;

import java.util.Iterator;
import java.util.Random;

import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.INewsConsumer;
import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.peersim.PermutingCache;
import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Node;

/**
 * The application layer is the one performing requests to and collecting
 * results from the {@link IApplicationInterface}. {@link SimpleApplication}
 * emulates such a layer, as well as the behavior of the user.<BR>
 * <BR>
 * It is in essence a simple, tunable traffic generator.
 * 
 * @author giuliano
 */
@AutoConfig
public class SimpleApplication implements CDProtocol, INewsConsumer {
	
	public static final int TWEET = 0;

	public static final int REPLY = 1;
	
	private static final int NONE = -1;

	@Attribute("social_newscasting_service")
	private int fSnService;

	private int fOneShot = NONE;
	
	private boolean fSuppress = false;

	private RouletteWheel fWheel;

	private PermutingCache fCache;

	public SimpleApplication(
			@Attribute("reply_probability") double replyProbability,
			@Attribute("tweet_probability") double tweetProbability,
			@Attribute("social_network") int linkable) {

		this(replyProbability, tweetProbability, linkable, CommonState.r);
	}

	public SimpleApplication(double replyProbability, double tweetProbability,
			int linkable, Random r) {
		if (tweetProbability + replyProbability > 1.0) {
			throw new IllegalArgumentException(
					"Tweet and reply probabilities cannot sum to more than one.");
		}

		double[] probs = new double[] { tweetProbability, replyProbability,
				(1.0 - (tweetProbability + replyProbability)) };

		fWheel = new RouletteWheel(probs, r);
		fCache = new PermutingCache(linkable);
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		switch (selectAction()) {
		case REPLY:
			// Tries to generate a reply. 
			Tweet tweet = pickTweet(node);
			if (tweet != null) {
				newscastingService().replyToPost(tweet);
				break;
			}
			// In case there's nothing to reply to, falls
			// back to tweeting.
			
		case TWEET:
			// Generates a tweet.
			newscastingService().postToFriends();
			break;
		
		default:
			// No traffic.
			break;
		}
	}
	
	public boolean isSuppressingTweets() {
		return fSuppress;
	}
	
	public boolean suppressTweeting(boolean status) {
		boolean before = fSuppress;
		fSuppress = status;
		return before;
	}
	
	public void scheduleOneShot(int action) {
		suppressTweeting(false);
		fOneShot = action;
	}
	
	public void setTrafficGeneratorSeed(long seed) {
		
	}
	
	/**
	 * Picks a tweet to post a reply to. The current algorithm simply picks the
	 * first tweet from a random friend.
	 * 
	 * @param node
	 *            the current node.
	 * 
	 * @return a tweet from a friend, or <code>null</code> if none is available.
	 */
	private Tweet pickTweet(Node node) {
		// Selects one friend of the node.
		fCache.populate(node);
		fCache.shuffle();

		IEventStorage storage = newscastingService().storage();

		for (int i = 0; i < fCache.size(); i++) {
			Node friend = fCache.get(i);
			Iterator<Tweet> twIt = storage.tweetsFor(friend);
			if (twIt.hasNext()) {
				return twIt.next();
			}
		}

		return null;
	}

	private int selectAction() {
		int action;
		if (fSuppress) {
			action = NONE;
		} else if (fOneShot != NONE) {
			action = fOneShot;
			fSuppress = true;
		} else {
			action = fWheel.spin();
		}
		fOneShot = NONE;
		return action;
	}

	private IApplicationInterface newscastingService() {
		return (IApplicationInterface) CommonState.getNode().getProtocol(
				fSnService);
	}

	@Override
	public void eventsReceived(Node sender, Node receiver, int start, int end) {
	}

	public Object clone() {
		try {
			SimpleApplication app = (SimpleApplication) super.clone();
			app.fCache = (PermutingCache) this.fCache.clone();
			app.fWheel = (RouletteWheel) this.fWheel.clone();

			return app;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
