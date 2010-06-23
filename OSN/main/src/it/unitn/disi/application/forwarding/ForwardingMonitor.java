package it.unitn.disi.application.forwarding;

import org.easymock.EasyMock;

import it.unitn.disi.IAdaptable;
import it.unitn.disi.utils.ConfigurationUtils;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Linkable;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

import static it.unitn.disi.utils.ConfigurationUtils.getAdaptable;

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
		
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			HistoryForwarding hf = getAdaptable(node, fAdaptableId, HistoryForwarding.class);
			fw.add(hf.queueSize());
			bfw.add(node);
		}
		
		// Print the statistics
		System.out.println("FWQUEUELENGTH:" + fw.toString());
		System.out.println("HITRATE: " + bfw.toString());
		
		if (bfw.isEmpty()) {
			return false;
		}
		
		// In the last round, dumps the hit rates for the caches.
		if (CommonState.getTime() == (CommonState.getEndTime() - 1)) {
			System.out.println("BEGIN_HITS:");
			
			for (int i = 0; i < Network.size(); i++) {
				Node node = Network.get(i);
				BloomFilterHistoryFw gd = getAdaptable(node, fAdaptableId, BloomFilterHistoryFw.class);
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
			
			HistoryForwarding fw = getAdaptable(node, fAdaptableId,
					HistoryForwarding.class);
			
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

