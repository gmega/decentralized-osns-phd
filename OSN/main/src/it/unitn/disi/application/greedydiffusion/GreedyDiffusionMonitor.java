package it.unitn.disi.application.greedydiffusion;

import it.unitn.disi.IAdaptable;
import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class GreedyDiffusionMonitor implements Control {
	
	private static final String PAR_ADAPTABLE = "adaptable";
	
	private static final String PAR_SNID = "social_network";

	private final int fAdaptableId;
	
	private final int fSnId;
	
	public GreedyDiffusionMonitor(String prefix) {
		fAdaptableId = Configuration.getPid(prefix + "." + PAR_ADAPTABLE);
		fSnId = Configuration.getPid(prefix + "." + PAR_SNID);
	}

	public boolean execute() {
		IncrementalStats stats = new IncrementalStats();

		Node worstRate = Network.get(0);
		double minRate = 1.0;
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			IAdaptable adaptable = (IAdaptable) node.getProtocol(fAdaptableId);
			
			GreedyDiffusion gd = (GreedyDiffusion) adaptable.getAdapter(GreedyDiffusion.class, null);
			stats.add(gd.queueSize());
			
			if (gd.cacheHitRate() < minRate) {
				worstRate = node;
				minRate = gd.cacheHitRate();
			}
		}
		
		// Print the statistics
		System.out.println("WORSTHIT: " + minRate + " " + worstRate.getID() + " " + getDegree(worstRate));
		System.out.println("GDQUEUE:" + stats.getMax() + " " + stats.getAverage());
		
		return false;
	}

	private int getDegree(Node worstRate) {
		Linkable linkable = (Linkable) worstRate.getProtocol(fSnId);
		return linkable.degree();
	}
}
