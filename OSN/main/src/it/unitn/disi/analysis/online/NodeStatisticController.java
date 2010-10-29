package it.unitn.disi.analysis.online;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

@AutoConfig
public class NodeStatisticController implements Control {

	@Attribute("protocol")
	private int fProtocol;
	
	private long fLastTime = 0;
	
	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			Node current = Network.get(i);
			NodeStatistic stat = (NodeStatistic) current.getProtocol(fProtocol);
			stat.advanceTime(fLastTime);
		}
		
		fLastTime = CommonState.getTime();
		
		return false;
	}

}
