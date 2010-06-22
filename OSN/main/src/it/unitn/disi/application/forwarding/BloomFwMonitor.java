package it.unitn.disi.application.forwarding;

import it.unitn.disi.application.IAdaptable;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class BloomFwMonitor implements Control {
	
	private static final String PAR_ADAPTABLE = "adaptable";
	
	private static final String PAR_SNID = "social_network";

	private final int fAdaptableId;
	
	private final int fSnId;
	
	public BloomFwMonitor(String prefix) {
		fAdaptableId = Configuration.getPid(prefix + "." + PAR_ADAPTABLE);
		fSnId = Configuration.getPid(prefix + "." + PAR_SNID);
	}

	public boolean execute() {
		IncrementalStats stats = new IncrementalStats();

		Node worstRate = Network.get(0);
		double minRate = 1.0;
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			BloomFilterHistoryFw gd = diffusionObject(node);
			stats.add(gd.queueSize());
			
			if (gd.cacheHitRate() < minRate) {
				worstRate = node;
				minRate = gd.cacheHitRate();
			}
		}
		
		// Print the statistics
		System.out.println("WORSTHIT: " + minRate + " " + worstRate.getID() + " " + getDegree(worstRate));
		System.out.println("GDQUEUE:" + stats.getMax() + " " + stats.getAverage());
		
		// In the last round, dumps the hit rates for the caches.
		if (CommonState.getTime() == (CommonState.getEndTime() - 1)) {
			System.out.println("BEGIN_HITS:");
			
			for (int i = 0; i < Network.size(); i++) {
				Node node = Network.get(i);
				BloomFilterHistoryFw gd = diffusionObject(node);
				System.out.println(node.getID() + " " + gd.cacheHitRate());
			}
			
			System.out.println("END_HITS:");
		}
		
		return false;
	}

	private BloomFilterHistoryFw diffusionObject(Node node) {
		IAdaptable adaptable = (IAdaptable) node.getProtocol(fAdaptableId);
		
		BloomFilterHistoryFw gd = (BloomFilterHistoryFw) adaptable.getAdapter(BloomFilterHistoryFw.class, null);
		return gd;
	}

	private int getDegree(Node worstRate) {
		Linkable linkable = (Linkable) worstRate.getProtocol(fSnId);
		return linkable.degree();
	}
}
