package it.unitn.disi.network.churn.yao;

import it.unitn.disi.random.IDistribution;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
import peersim.core.Fallible;
import peersim.core.Node;

/**
 * <a href="http://dx.doi.org/10.1109/ICNP.2006.320196">Yao's on-off churn
 * model</a> assigns an ON/OFF renewal process to each node in the network.<BR>
 * <BR>
 * Downtime and uptime are drawn from separate, arbitrary distributions F_{i}
 * and G_{i}, which are differ for each node, thus allowing heterogeneity to be
 * modeled. <BR>
 * <BR>
 * Instances of {@link YaoOnOffChurn} must be initialized by calling
 * {@link #init(IDistribution, IDistribution, Node)} on each nodebefore the
 * simulation starts. Initializers should supply {@link IDistribution} objects
 * for uptime and downtime. <BR>
 * Values returned by {@link IDistribution#sample()} are converted to simulation
 * time by multiplying it by a time scale parameter, which should be supplied
 * during initial configuration.
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
		this.scheduleTransition(uptime(node), node, NodeState.ON, NodeState.OFF);
	}

	private void scheduleDowntime(Node node) {
		// Takes node down.
		node.setFailState(Fallible.DOWN);
		// Schedules downtime.
		this.scheduleTransition(downtime(node), node, NodeState.OFF,
				NodeState.ON);
	}

	private long downtime(Node node) {
		return (long) Math.ceil(fScaling * fOff.sample());
	}

	private long uptime(Node node) {
		return (long) Math.ceil(fScaling * fOn.sample());
	}

}

enum NodeState {
	ON, OFF
}
