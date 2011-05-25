package it.unitn.disi.newscasting.internal.forwarding;

import it.unitn.disi.epidemics.IProtocolSet;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

public class ForwardingMonitor implements Control {
	
	private static final String PAR_ADAPTABLE = "adaptable";
	
	private static final String PAR_SNID = "social_network";

	private final int fAdaptableId;
	
	private final int fSnId;
	
	public ForwardingMonitor(String prefix) {
		fAdaptableId = Configuration.getPid(prefix + "." + PAR_ADAPTABLE);
		fSnId = Configuration.getPid(prefix + "." + PAR_SNID);
	}

	public boolean execute() {

		IncrementalStats fw = new IncrementalStats();
		BloomFwStats bfw = new BloomFwStats();
		int totalEntries = 0;
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			IProtocolSet intf = (IProtocolSet) node.getProtocol(fAdaptableId);
			HistoryForwarding hf = intf.getStrategy(HistoryForwarding.class);
			fw.add(hf.queueSize());
			totalEntries += hf.queueSize();
			bfw.add(node);
		}
		
		// Print the statistics
		System.out.println("FWQUEUELENGTH:" + fw.toString());
		System.out.println("TOTALBUFFEREDMSG: " + totalEntries);
		
		if (bfw.isEmpty()) {
			return false;
		}
		
		System.out.println("HITRATE: " + bfw.toString());
		
		// In the last round, dumps the hit rates for the caches.
		if (CommonState.getTime() == (CommonState.getEndTime() - 1)) {
			System.out.println("BEGIN_HITS:");
			
			for (int i = 0; i < Network.size(); i++) {
				Node node = Network.get(i);
				IProtocolSet intf = (IProtocolSet) node.getProtocol(fAdaptableId);				
				BloomFilterHistoryFw gd = (BloomFilterHistoryFw) intf.getStrategy(HistoryForwarding.class);
				System.out.println(node.getID() + " " + gd.cacheHitRate());
			}
			
			System.out.println("END_HITS:");
		}
		
		return false;
	}
	
	class BloomFwStats {
		
		private IncrementalStats fStats = new IncrementalStats();
		private boolean fInit = false;
		private Node fWorst;

		public void add(Node node) {
			
			IProtocolSet intf = (IProtocolSet) node
					.getProtocol(fAdaptableId);
			HistoryForwarding fw = intf.getStrategy(HistoryForwarding.class);

			if (!(fw instanceof BloomFilterHistoryFw)) {
				return;
			}
			
			BloomFilterHistoryFw fh = (BloomFilterHistoryFw) fw;
			
			double hitRate = fh.cacheHitRate();
			if (hitRate < fStats.getMin() || !fInit) {
				fWorst = node;
				fInit = true;
			}
			
			fStats.add(hitRate);
		}
		
		public Node getWorst() {
			return fWorst;
		}
		
		public int getWorstDegree() {
			Linkable linkable = (Linkable) fWorst.getProtocol(fSnId);
			return linkable.degree();
		}
		
		public boolean isEmpty() {
			return !fInit;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(fStats.getMin());
			buffer.append(" ");
			buffer.append(fStats.getMax());
			buffer.append(" ");
			buffer.append(fStats.getAverage());
			buffer.append(" ");
			buffer.append(fStats.getStD());
			buffer.append(" ");
			buffer.append(getWorstDegree());
			
			return buffer.toString();
		}
	}
}

