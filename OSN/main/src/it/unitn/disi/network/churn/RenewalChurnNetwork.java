package it.unitn.disi.network.churn;

import peersim.config.IResolver;
import peersim.core.Node;

/**
 * Base class for implementing churn models which assign a renewal process to
 * each node.
 * 
 * @author giuliano
 */
public abstract class RenewalChurnNetwork extends
		SemiMarkovChurnNetwork<OnOffState> {

	protected RenewalChurnNetwork(String prefix, IResolver resolver) {
		super(prefix, resolver);
	}

	protected RenewalChurnNetwork(int selfPid, String prefix, IResolver resolver) {
		super(selfPid, prefix, resolver);
	}

	@Override
	protected void stateChanged(Node node, Delta<OnOffState> state) {
		processState(node, state.next);
	}

	protected void processState(Node node, OnOffState state) {
		switch (state) {

		case OFF:
			scheduleDowntime(node);
			break;

		case ON:
			scheduleUptime(node);
			break;

		}
	}

	private void scheduleUptime(Node node) {
		// Takes the node back up.
		restart(node);
		// Schedules uptime.
		this.scheduleTransition(uptime(node), node, OnOffState.ON,
				OnOffState.OFF);
	}

	private void scheduleDowntime(Node node) {
		// Takes node down.
		takedown(node);
		// Schedules downtime, if node hasn't departed. Otherwise we stop
		// scheduling events for this node.
		if (!hasDeparted(node)) {
			this.scheduleTransition(downtime(node), node, OnOffState.OFF,
					OnOffState.ON);
		}
	}

	protected abstract long downtime(Node node);

	protected abstract long uptime(Node node);

	protected abstract boolean hasDeparted(Node node);

}
