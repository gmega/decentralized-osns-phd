package it.unitn.disi.newscasting.experiments;

import peersim.core.Node;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.SNNode;

/**
 * External controller which halts dissemination after a certain time amount has
 * passed. Only suitable within the unit experiment framework.
 * 
 * @author giuliano
 */
public class TimeoutExpiration implements IEventObserver, IExperimentObserver {

	private final IReference<IContentExchangeStrategy> fXchgRef;
	
	private final int fTimeReserve;

	private int fClock = -1;

	public TimeoutExpiration(int timeout, IReference<IContentExchangeStrategy> xchgRef) {
		if (timeout == 0) {
			throw new IllegalArgumentException(
					"Timeout must be greater than zero.");
		}
		fTimeReserve = timeout;
		fXchgRef = xchgRef;
	}

	@Override
	public void tweeted(Tweet tweet) {
		startClock();
	}

	@Override
	public void eventDelivered(SNNode sender, SNNode receiver, Tweet tweet,
			boolean duplicate) {
		if (!duplicate) {
			startClock();
		}
	}

	@Override
	public void experimentCycled(Node root) {
		clockTick();
		if (timeout()) {
			shutdownNode(root);
		}
	}

	protected void shutdownNode(Node node) {
		IContentExchangeStrategy strategy = fXchgRef.get(node);
		strategy.clear(node);
	}

	@Override
	public void experimentStart(Node root) {
	}

	@Override
	public void experimentEnd(Node root) {
	}

	private void clockTick() {
		fClock--;
	}

	private void startClock() {
		fClock = fTimeReserve;
	}

	private boolean timeout() {
		return fClock == 0;
	}
}
