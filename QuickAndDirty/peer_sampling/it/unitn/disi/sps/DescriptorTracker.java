package it.unitn.disi.sps;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class DescriptorTracker implements Control {
	
	@Attribute
	private int linkable;
	
	@Attribute
	private long descriptor;

	@Override
	public boolean execute() {
		boolean found = false;
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			Linkable lnk = (Linkable) node.getProtocol(linkable);
			boolean has = false;
			CommonState.setNode(node);
			for (int j = 0; j < lnk.degree(); j++) {
				Node neighbor = lnk.getNeighbor(j);
				if (neighbor.getID() == descriptor) {
					found = has = true;
					break;
				}
			}
			
			if (has) {
				System.out.println(DescriptorTracker.class + ": " + i + " " + CommonState.getTime());
			}
		}
		
		if (!found) {
			System.out.println(DescriptorTracker.class + ": descriptor " + descriptor + " not found.");
		}
		
		return false;
	}

}
