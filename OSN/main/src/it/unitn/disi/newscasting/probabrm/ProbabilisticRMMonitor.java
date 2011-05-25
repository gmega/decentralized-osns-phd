package it.unitn.disi.newscasting.probabrm;

import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.utils.peersim.PeersimUtils;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class ProbabilisticRMMonitor implements Control {
	
	private static final String PAR_ADAPTABLE = "adaptable";

	private final int fAdaptableId;
	
	public ProbabilisticRMMonitor(String prefix) {
		fAdaptableId = Configuration.getPid(prefix + "." + PAR_ADAPTABLE);
	}
	
	public boolean execute() {

		if (CommonState.getTime() == 1000) {
			System.out.println("DUMPPENDING:");
		}
		
		IncrementalStats stats = new IncrementalStats();
		int total = 0;
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			IProtocolSet adaptable = (IProtocolSet) node.getProtocol(fAdaptableId);
			ProbabilisticRumorMonger prm = (ProbabilisticRumorMonger) adaptable
					.getStrategy(ProbabilisticRumorMonger.class);
			
			if (CommonState.getTime() == 1000) {
				System.out.println(node.getID() + " " + prm.pendingRounds());
			}
			
			total += prm.pendingRounds();
			stats.add(prm.queueLength());
		}
		
		if (CommonState.getTime() == 1000) {
			System.out.println("ENDDUMPPENDING:");
		}
		
		PeersimUtils.print("Qlength", stats);
		System.out.println("TotalR: " + total);
		
		return false;
	}

}
