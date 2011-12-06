package it.unitn.disi.network.churn;

import it.unitn.disi.utils.peersim.SNNode;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;


@AutoConfig
public class SessionAverages implements Control {

	@Override
	public boolean execute() {
		for (int i = 0; i < Network.size(); i++) {
			StringBuffer buffer = new StringBuffer();
			SNNode node = (SNNode) Network.get(i);
			buffer.append("P");
			buffer.append(i);
			buffer.append(":");
			buffer.append(node.downtime(false));
			buffer.append(" ");
			buffer.append(node.downtimeN(false));
			buffer.append(" ");
			buffer.append(node.uptime(false));
			buffer.append(" ");
			buffer.append(node.uptimeN(false));
			buffer.append(" ");
			buffer.append(CommonState.getTime());
			System.out.println(buffer);
		}
		
		return false;
	}

}
