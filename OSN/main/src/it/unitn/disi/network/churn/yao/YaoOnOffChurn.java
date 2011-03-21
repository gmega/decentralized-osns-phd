package it.unitn.disi.network.churn.yao;

import it.unitn.disi.random.IDistribution;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Fallible;
import peersim.core.Node;

/**
 * Yao's on-off churn model.
 * 
 * @author giuliano
 */
@AutoConfig
public class YaoOnOffChurn extends OnOffChurnNetwork<NodeState> { 

	private IDistribution fOn;

	private IDistribution fOff;

	private final double fScaling;

	public YaoOnOffChurn(@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute(value = "timescale") double scaling) {
		super(prefix, resolver);
		fScaling = scaling;
	}

	public void init(IDistribution on, IDistribution off, Node node) {
		fOn = on;
		fOff = off;
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
		double dt = fScaling * fOff.sample();
		System.out.println("D:" + dt);
		return Math.round(dt);
	}

	private long uptime(Node node) {
		double ut = fScaling * fOn.sample();
		System.out.println("U:" + ut);
		return Math.round(ut);
	}

}

enum NodeState {
	ON, OFF
}
