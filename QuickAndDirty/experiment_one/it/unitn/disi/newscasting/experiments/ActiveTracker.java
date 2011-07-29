package it.unitn.disi.newscasting.experiments;

import it.unitn.disi.epidemics.IContentExchangeStrategy.ActivityStatus;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.newscasting.internal.forwarding.HistoryForwarding;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class ActiveTracker implements Control {

	@Attribute
	private int protocol;

	@Override
	public boolean execute() {
		int active = 0;
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			IProtocolSet set = (IProtocolSet) node.getProtocol(protocol);
			HistoryForwarding hist = set.getStrategy(HistoryForwarding.class);
			if (hist.status() == ActivityStatus.ACTIVE) {
				active++;
			}
		}
		
		System.out.println("ACTIVE:" + active);
		return false;
	}

}
