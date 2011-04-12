package it.unitn.disi.newscasting.experiments.churn;

import peersim.config.Attribute;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.peersim.SNNode;

public class TimeoutReset implements IEventObserver {
	
	private IReference<AbstractTimeoutController> fController;
	
	public TimeoutReset(@Attribute("timeout_controller") int id) {
		fController = new ProtocolReference<AbstractTimeoutController>(id);
	}

	@Override
	public void tweeted(Tweet tweet) {
		fController.get(tweet.poster).startTimeout(tweet.poster);
	}

	@Override
	public void eventDelivered(SNNode sender, SNNode receiver, Tweet tweet,
			boolean duplicate) {
		
		// Starts the timeout at the receiver, if required.
		if (!duplicate) {
			fController.get(receiver).startTimeout(tweet.poster);
		}
		
		// And restarts the timeout at the sender.
		fController.get(sender).startTimeout(tweet.poster);
	}

}
