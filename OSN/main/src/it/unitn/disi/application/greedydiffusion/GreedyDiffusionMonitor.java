package it.unitn.disi.application.greedydiffusion;

import it.unitn.disi.IAdaptable;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class GreedyDiffusionMonitor implements Control {
	
	private static final String PAR_ADAPTABLE = "adaptable";

	private final int fAdaptableId;
	
	public GreedyDiffusionMonitor(String prefix) {
		fAdaptableId = Configuration.getPid(prefix + "." + PAR_ADAPTABLE);
	}

	public boolean execute() {
		IncrementalStats stats = new IncrementalStats();
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			IAdaptable adaptable = (IAdaptable) node.getProtocol(fAdaptableId);
			
			GreedyDiffusion gd = (GreedyDiffusion) adaptable.getAdapter(GreedyDiffusion.class, null);
			stats.add(gd.queueSize());
		}
		
		System.out.println("GDQUEUE:" + stats.getMax() + " " + stats.getAverage());
		return false;
	}
}
