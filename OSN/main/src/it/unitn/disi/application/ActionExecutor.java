package it.unitn.disi.application;

import java.util.Iterator;

import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.PermutingCache;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

/**
 * Executes pre-scheduled traffic generation actions (or, more generally,
 * pre-scheduled {@link IAction}s).
 * 
 * @author giuliano
 */
@AutoConfig
public class ActionExecutor implements EDProtocol<IAction> {

	private static int fSelfPid;

	private static int fSnService;

	private static int fNeighborhood;

	private static PermutingCache fCache;

	public static final IAction TWEET = new IAction() {
		@Override
		public void execute(Node node) {
			// Generates a tweet.
			newscastingService(node).postToFriends();
		}
	};

	public static final IAction REPLY_RANDOM = new IAction() {
		@Override
		public void execute(Node node) {
			// Tries to generate a reply.
			Tweet tweet = pickTweet(node);
			if (tweet != null) {
				newscastingService(node).replyToPost(tweet);
			}
		}
	};

	private static void oneShotInit(int neighborhood, int snService, int selfPid) {
		fNeighborhood = neighborhood;
		fSnService = snService;
		fSelfPid = selfPid;
		fCache = new PermutingCache(fNeighborhood);
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
	private static Tweet pickTweet(Node node) {
		// Selects one friend of the node.
		fCache.populate(node);
		fCache.shuffle();

		IEventStorage storage = newscastingService(node).storage();

		for (int i = 0; i < fCache.size(); i++) {
			Node friend = fCache.get(i);
			Iterator<Tweet> twIt = storage.tweetsFor(friend);
			if (twIt.hasNext()) {
				return twIt.next();
			}
		}

		return null;
	}

	private static IApplicationInterface newscastingService(Node node) {
		return (IApplicationInterface) node.getProtocol(fSnService);
	}

	public ActionExecutor(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("neighborhood") int neighborhood,
			@Attribute("social_newscasting_service") int snService) {
		oneShotInit(neighborhood, snService, PeersimUtils.selfPid(prefix));
	}

	public void add(long delay, Node node, IAction action) {
		if (EDSimulator.isConfigurationEventDriven()) {
			EDSimulator.add(delay, action, node, fSelfPid);
		} else {
			CDActionScheduler.add(delay, action, node, fSelfPid);
		}
	}

	@Override
	public void processEvent(Node node, int pid, IAction event) {
		event.execute(node);
	}

	public Object clone() {
		return this;
	}
}
