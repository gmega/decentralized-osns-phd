package it.unitn.disi.sps;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

/**
 * {@link DescriptorObserver} allows us to search for nodes which carry a given
 * descriptor.
 * 
 * @author giuliano
 */
@AutoConfig
public class DescriptorObserver implements Control {

	private int fNeighborhood;

	private long[] fSpecials;

	public DescriptorObserver(@Attribute("specials") String specials,
			@Attribute("linkable") int neighborhoodId) {

		String[] parts = specials.split(" ");
		fSpecials = new long[parts.length];
		for (int i = 0; i < parts.length; i++) {
			fSpecials[i] = Long.parseLong(parts[i]);
		}
		fNeighborhood = neighborhoodId;
	}

	@Override
	public boolean execute() {
		StringBuffer buffer = new StringBuffer();
		boolean print = false;
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			Linkable neighborhood = (Linkable) node.getProtocol(fNeighborhood);
			print |= linkableContains(node, neighborhood, buffer);
		}

		if (print) {
			System.out.println("B_" + DescriptorObserver.class.getSimpleName()
					+ " " + CommonState.getTime());
			System.out.println(buffer);
			System.out.println("E_" + DescriptorObserver.class.getSimpleName()
					+ " " + CommonState.getTime());
		}

		return false;
	}

	private boolean linkableContains(Node node, Linkable linkable,
			StringBuffer buffer) {
		boolean foundSomething = false;
		for (long special : fSpecials) {
			for (int i = 0; i < linkable.degree(); i++) {
				Node candidate = linkable.getNeighbor(i);
				if (candidate.getID() == special) {
					buffer.append(node.getID());
					buffer.append(" ");
					buffer.append(special);
					buffer.append("\n");
					foundSomething = true;
				}
			}
		}
		return foundSomething;
	}

}
