package it.unitn.disi.newscasting.experiments.schedulers;

import it.unitn.disi.network.churn.yao.YaoOnOffChurn;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.peersim.ProtocolReference;

import java.util.Comparator;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;

/**
 * {@link ScheduleByAvailability} orders node by availability (lowest first).
 * 
 * @author giuliano
 */
@AutoConfig
public class ScheduleByAvailability implements Comparator<Node> {

	private final IReference<YaoOnOffChurn> fProtocol;

	public ScheduleByAvailability(@Attribute("protocol") int protocolId) {
		fProtocol = new ProtocolReference<YaoOnOffChurn>(protocolId);
	}

	@Override
	public int compare(Node o1, Node o2) {
		YaoOnOffChurn y1 = fProtocol.get(o1);
		YaoOnOffChurn y2 = fProtocol.get(o2);
		return (int) Math.signum(y1.availability() - y2.availability());
	}
}
