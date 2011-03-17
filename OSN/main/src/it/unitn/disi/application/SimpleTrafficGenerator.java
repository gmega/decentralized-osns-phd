package it.unitn.disi.application;

import it.unitn.disi.util.RouletteWheel;

import java.util.Random;

import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * Simple traffic generator which queues {@link TrafficScheduler#TWEET} and
 * {@link TrafficScheduler#REPLY_RANDOM} events with equal probability.
 * 
 * @author giuliano
 */
@AutoConfig
public class SimpleTrafficGenerator implements CDProtocol {

	private static final IAction[] fActions = { TrafficScheduler.TWEET,
			TrafficScheduler.REPLY_RANDOM };

	@Attribute("action_scheduler")
	private int fActionScheduler;

	private double[] fProbabs;

	private boolean fSuppress = false;

	private RouletteWheel fWheel;

	public SimpleTrafficGenerator(
			@Attribute("reply_probability") double replyProbability,
			@Attribute("tweet_probability") double tweetProbability) {
		this(replyProbability, tweetProbability, CommonState.r);
	}

	public SimpleTrafficGenerator(double replyProbability,
			double tweetProbability, Random r) {
		if (tweetProbability + replyProbability > 1.0) {
			throw new IllegalArgumentException(
					"Tweet and reply probabilities cannot sum to more than one.");
		}

		fProbabs = new double[] { tweetProbability, replyProbability,
				(1.0 - (tweetProbability + replyProbability)) };

		fWheel = new RouletteWheel(fProbabs, r);
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		IAction action = selectAction();
		if (action != null) {
			EDSimulator.add(0, action, node, fActionScheduler);
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

	public void setTrafficGeneratorSeed(long seed) {
		fWheel = new RouletteWheel(fProbabs, new Random(seed));
	}

	private IAction selectAction() {
		if (fSuppress) {
			return null;
		}
		return fActions[fWheel.spin()];
	}

	public Object clone() {
		try {
			SimpleTrafficGenerator app = (SimpleTrafficGenerator) super.clone();
			app.fWheel = (RouletteWheel) this.fWheel.clone();
			return app;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
