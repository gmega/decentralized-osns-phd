package it.unitn.disi.newscasting.experiments.f2f;

import java.util.Iterator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Linkable;
import peersim.core.Node;
import it.unitn.disi.f2f.DiscoveryProtocol;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.NodeRegistry;

@AutoConfig
public class AddRemoveNetwork extends GrowingBFSNetwork {

	enum State {
		growing, changing
	}

	private State fState;

	public AddRemoveNetwork(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("graph") int graphProtocolId,
			@Attribute("join") int discoveryId, @Attribute("seed") int seed,
			@Attribute("TabularLogManager") TabularLogManager manager) {
		super(prefix, graphProtocolId, discoveryId, seed, manager);
		fState = State.growing;
	}

	@Override
	public boolean execute() {

		if (isJoining()) {
			return false;
		}

		switch (fState) {
		case growing:
			if (super.execute()) {
				fState = State.changing;
				super.iteratorReset();
			}
			return false;
			
		case changing:
			return reAddNode();
		}

		return true;
	}

	private boolean reAddNode() {
		Iterator<Pair<Integer, Integer>> it = iterator();
		if (!it.hasNext()) {
			return true;
		}
		
		// Removes a node from the network. This amounts to:
		Pair<Integer, Integer> nextId = it.next();
		Node next = NodeRegistry.getInstance().getNode(nextId.a);

		// 1. clearing from neighbors the descriptors pointing to this node.
		Linkable neighborhood = (Linkable) next.getProtocol(fGraphProtocolId);
		for (int i = 0; i < neighborhood.degree(); i++) {
			Node neighbor = neighborhood.getNeighbor(i);
			DiscoveryProtocol neighborDiscovery = (DiscoveryProtocol) neighbor
					.getProtocol(fDiscoveryId);
			neighborDiscovery.removeNeighbor(next);
		}

		// 2. clearing all of the descriptors for this node.
		DiscoveryProtocol root = (DiscoveryProtocol) next
				.getProtocol(fDiscoveryId);
		root.clearState();
		root.resetStatistics();
		
		System.err.println("-- Scheduled node " + next.getID() + ".");
		
		// 3. now reschedules it.
		doSchedule(next);
		
		return false;
	}

}
