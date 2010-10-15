package it.unitn.disi.sps;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Dumps the graph that is complementary to what's been built by the
 * {@link F2FOverlayCollector} until a predefined point in time.
 * 
 * @author giuliano
 */
@AutoConfig
public class MissingList implements Control {

	@Attribute("linkable")
	private int fTarget;

	@Attribute("collector")
	private int fCollectorId;

	@Override
	public boolean execute() {
		if (CommonState.getTime() == 0) {
			return false;
		}
		int size = Network.size();
		System.out.println("B_" + MissingList.class.getSimpleName());
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			dump(node);
		}
		System.out.println("E_" + MissingList.class.getSimpleName());
		
		return false;
	}

	private void dump(Node node) {
		Linkable target = (Linkable) node.getProtocol(fTarget);
		F2FOverlayCollector collector = (F2FOverlayCollector) node
				.getProtocol(fCollectorId);

		int degree = target.degree();
		for (int i = 0; i < degree; i++) {
			if (!collector.seen(i)) {
				Node unseen = target.getNeighbor(i);
				System.out.println(node.getID() + " " + unseen.getID());
			}
		}
	}

}
