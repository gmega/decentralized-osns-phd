package it.unitn.disi.network;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;

/**
 * Simple churn model which keeps a pool of dead nodes. At each round, a number
 * of random nodes leaves the pool, and an equal number enters the pool. Nodes
 * are chosen uniformly at random, both from the pool and from the network.
 * 
 * @author giuliano
 */
@AutoConfig
public class FixedSizeNetwork extends AbstractUniformChurnNetwork {

	/**
	 * Percentage of the network that will be down at all times.
	 */
	@Attribute("dead_pool")
	private double fDeadPoolSize;

	/**
	 * Percentage of the network to be replaced at each round.
	 */
	@Attribute("churn_rate")
	private double fChurnRate;

	public FixedSizeNetwork(@Attribute(Attribute.PREFIX) String prefix) {
		super(prefix, true);
	}

	@Override
	public boolean execute() {
		this.cycleStarted();
		int size = networkSize();

		// At each execution:
		int deathQuota = deathQuota(size);
		int turnover = turnover(size);

		// 1. Takes some random nodes back to life.
		for (int i = 0; i < turnover && DEAD.size() > 0; i++) {
			DEAD.getRandom();
			resurrect();
		}

		// 2. Kills some random nodes.
		int eligible = ALIVE.size();
		while(DEAD.size() < deathQuota && eligible > 0) {
			eligible--;
			Node candidate = ALIVE.getRandom();
			if (!canDie(candidate)) {
				continue;
			}
			kill();
		}

		return false;
	}

	protected int turnover(int size) {
		return (int) Math.floor(fChurnRate * size);
	}

	protected int deathQuota(int size) {
		return (int) Math.floor(fDeadPoolSize * size);
	}
		
	protected boolean canDie(Node node) {
		return node.isUp();
	}
}
