package it.unitn.disi.network.churn.bucketed;

import it.unitn.disi.utils.collections.StaticVector;
import it.unitn.disi.utils.peersim.NodeRebootSupport;
import peersim.config.Attribute;
import peersim.core.Control;
import peersim.core.GeneralNode;
import peersim.core.Network;
import peersim.core.Node;

public abstract class BucketedChurnNetwork implements Control {

	protected final NetworkPartition DEAD = new NetworkPartition();

	protected final NetworkPartition ALIVE = new NetworkPartition();

	private final NodeRebootSupport fReboot;

	private boolean fPartition;

	public BucketedChurnNetwork(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("partition") boolean partition) {
		fReboot = new NodeRebootSupport(prefix);
		fPartition = partition;
	}

	public void cycleStarted() {
		DEAD.ensureSize(networkSize());
		ALIVE.ensureSize(networkSize());
		// Bootstrap partitioning, if requested.
		if (fPartition) {
			partition();
			fPartition = false;
		}
	}

	protected void partition() {
		int size = networkSize();
		for (int i = 0; i < size; i++) {
			Node node = get(i);
			if (node.isUp()) {
				ALIVE.add(node);
			} else {
				DEAD.add(node);
			}
		}
	}

	protected void kill() {
		Node killed = ALIVE.removeLastReturned();
		killed.setFailState(GeneralNode.DOWN);
		DEAD.add(killed);
	}

	protected void resurrect() {
		Node resurrected = DEAD.removeLastReturned();
		resurrected.setFailState(GeneralNode.OK);
		fReboot.initialize(resurrected);
		ALIVE.add(resurrected);
	}

	protected Node get(int index) {
		return Network.get(index);
	}

	protected int networkSize() {
		return Network.size();
	}

	protected class NetworkPartition {

		private final StaticVector<Node> fNodes;

		private boolean fNeedsShuffling;

		private int fNeedsCompacting = 0;

		private int fLast;

		private NetworkPartition() {
			fNodes = new StaticVector<Node>();
		}

		private void add(Node node) {
			compactIfNeeded();
			fNodes.append(node);
			fNeedsShuffling = true;
		}

		public Node selectRandom() {
			if (fNeedsShuffling) {
				compactIfNeeded();
				fNodes.permute();
				fLast = fNodes.size();
				fNeedsShuffling = false;
			}
			
			if (fLast == 0) {
				return null;
			}

			fLast--;
			return fNodes.get(fLast);
		}

		public int size() {
			return fNodes.size() - fNeedsCompacting;
		}

		private Node removeLastReturned() {
			Node node = fNodes.nullOut(fLast);
			fNeedsCompacting++;
			return node;
		}

		private void ensureSize(int size) {
			compactIfNeeded();
			fNodes.resize(size, true);
		}

		private void compactIfNeeded() {
			if (fNeedsCompacting > 0) {
				fNodes.compact();
				fNeedsCompacting = 0;
			}
		}
	}
}
