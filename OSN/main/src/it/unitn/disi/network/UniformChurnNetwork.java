package it.unitn.disi.network;

import java.util.BitSet;

import it.unitn.disi.utils.collections.StaticVector;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.GeneralNode;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Simple churn model which keeps a pool of dead nodes. At each round, a number
 * of random nodes leaves the pool, and an equal number enters the pool. Nodes
 * are chosen uniformly at random, both from the pool and from the network.
 * 
 * @author giuliano
 */
@AutoConfig
public class UniformChurnNetwork implements Control {

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

	private final NodeRebootSupport fReboot;

	private final StaticVector<Node> fDead;

	private final BitSet fWoken;

	public UniformChurnNetwork(@Attribute(Attribute.PREFIX) String prefix) {
		fReboot = new NodeRebootSupport(prefix);
		fDead = new StaticVector<Node>();
		fWoken = new BitSet();
	}

	@Override
	public boolean execute() {
		int size = networkSize();
		fDead.resize(size, false);
		fWoken.clear();

		// At each execution:
		int deathQuota = (int) Math.floor(fDeadPoolSize * size);
		int turnover = (int) Math.floor(fChurnRate * size);

		// 1. Takes some random nodes back to life.
		fDead.permute();
		for (int i = 0; i < turnover && fDead.size() > 0; i++) {
			Node resurrected = fDead.removeLast();
			resurrected.setFailState(GeneralNode.OK);
			fReboot.initialize(resurrected);
			fWoken.set((int) resurrected.getID());
		}

		// 2. Kills some random nodes.
		shuffleNetwork();
		for (int i = 0; i < size && fDead.size() < deathQuota; i++) {
			Node candidate = Network.get(i);
			if (!canDie(candidate)) {
				continue;
			}
			candidate.setFailState(GeneralNode.DOWN);
			fDead.append(candidate);
		}

		return false;
	}

	protected void clearState() {
		fDead.clear();
		fWoken.clear();
	}

	protected int networkSize() {
		return Network.size();
	}

	protected Node get(int index) {
		return Network.get(index);
	}

	protected void shuffleNetwork() {
		Network.shuffle();
	}

	protected boolean canDie(Node node) {
		return !fWoken.get((int) node.getID()) && node.isUp();
	}
}
