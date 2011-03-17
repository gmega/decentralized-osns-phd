package it.unitn.disi.network.churn.yao;

import it.unitn.disi.random.IDistribution;
import peersim.config.Attribute;
import peersim.config.IResolver;
import peersim.core.Fallible;
import peersim.core.Node;

/**
 * Yao's on-off churn model.
 * 
 * @author giuliano
 */
public class YaoOnOffChurn extends OnOffChurnNetwork<NodeState> {

	private IDistribution fOn;

	private IDistribution fOff;

	private double fScaling;

	public YaoOnOffChurn(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix) {
		super(prefix, resolver);
	}

	public void init(IDistribution on, IDistribution off, double scaling,
			Node node) {
		fOn = on;
		fOff = off;
		fScaling = scaling;
		processState(node, NodeState.OFF);
	}

	@Override
	protected void stateChanged(Node node, Delta<NodeState> state) {
		processState(node, state.next);
	}

	private void processState(Node node, NodeState state) {
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
		node.setFailState(Fallible.OK);
		// Initializes the node.
		reinit(node);
		// Schedules uptime.
		this.scheduleTransition(uptime(node), node, NodeState.ON,
				NodeState.OFF);
	}

	private void scheduleDowntime(Node node) {
		// Takes node down.
		node.setFailState(Fallible.DOWN);
		// Schedules downtime.
		this.scheduleTransition(downtime(node), node, NodeState.OFF,
				NodeState.ON);
	}

	private long downtime(Node node) {
		return Math.round(fScaling * fOff.sample());
	}

	private long uptime(Node node) {
		return Math.round(fScaling * fOn.sample());
	}

}

enum NodeState {
	ON, OFF
}
