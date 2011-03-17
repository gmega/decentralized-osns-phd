package it.unitn.disi.application;

import it.unitn.disi.newscasting.IApplicationInterface;
import it.unitn.disi.newscasting.IEventStorage;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.PermutingCache;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
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
public class TrafficScheduler implements EDProtocol<IAction> {
	
	private static IReference<IScheduler<IAction>> fScheduler;

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

	private static void oneShotInit(int neighborhood, int snService,
			int selfPid, int scheduler) {
		fNeighborhood = neighborhood;
		fSnService = snService;
		fSelfPid = selfPid;
		fScheduler = new ProtocolReference<IScheduler<IAction>>(scheduler);
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

	public TrafficScheduler(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("neighborhood") int neighborhood,
			@Attribute("social_newscasting_service") int snService,
			@Attribute("scheduler") int schedulerService) {
		oneShotInit(neighborhood, snService, PeersimUtils.selfPid(prefix),
				schedulerService);
	}

	public void add(long delay, Node node, IAction action) {
		IScheduler<IAction> scheduler = fScheduler.get(node);
		scheduler.schedule(delay, fSelfPid, node, action);
	}

	@Override
	public void processEvent(Node node, int pid, IAction event) {
		event.execute(node);
	}

	public Object clone() {
		return this;
	}
}
