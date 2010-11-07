package it.unitn.disi.sps.debug;

import it.unitn.disi.newscasting.IPeerSelector;
import it.unitn.disi.utils.MultiCounter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;

/**
 * Simple application where each node keeps track of:
 * 
 * <ol>
 * <li>the number of times it has been contacted;
 * <li>the number of times it has contacted each of its neighbors.
 * </ol>
 * 
 * @author giuliano
 */
public class PeersamplingDebuggingApp implements CDProtocol {
	// --------------------------------------------------------------------------
	// Parameters
	// --------------------------------------------------------------------------

	/**
	 * Reference to an object implementing the {@link IPeerSelector}
	 * interface.
	 */
	private static final String PAR_PSS = "pss";

	/**
	 * Monitoring parameter. Should be a list of the form ID:ROUND. It will
	 * cause method {@link #isMonitored(Node)} to return true when:
	 * <ol>
	 * <li> {@link Node#getID()} == ID, and; </li>
	 * <li> {@link CommonState#getIntTime()} == ROUND. </li>
	 * </ol>
	 */
	private static final String PAR_MONITOR = "monitor";

	// --------------------------------------------------------------------------
	// Static fields.
	// --------------------------------------------------------------------------

	private static HashSet<Node> fTrim = new HashSet<Node>();

	private static Set<Long> fMonitor;

	private static Map<Long, Long> fRounds;
	
	// --------------------------------------------------------------------------
	// Parameter values.
	// --------------------------------------------------------------------------
	
	private int fPeersamplingId;

	// --------------------------------------------------------------------------
	// Protocol state.
	// --------------------------------------------------------------------------

	/**
	 * Tracks which neighbor has been contacted and how many times.
	 */
	private MultiCounter<Node> fCounter = new MultiCounter<Node>();

	private int fSeen;

	private int fContacted;
	
	// --------------------------------------------------------------------------

	public PeersamplingDebuggingApp(String prefix) {
		fPeersamplingId = Configuration.getPid(prefix + "." + PAR_PSS);
		
		if (Configuration.contains(prefix + "." + PAR_MONITOR)
				&& fMonitor == null) {
			String[] list = Configuration.getString(prefix + "." + PAR_MONITOR)
					.split(" ");
			fMonitor = new HashSet<Long>();
			fRounds = new HashMap<Long, Long>();
			for (int i = 0; i < list.length; i++) {
				String[] spec = list[i].split(":");
				long key = Long.parseLong(spec[0]);
				if (spec.length >= 1) {
					fMonitor.add(key);
				}

				if (spec.length >= 2) {
					fRounds.put(key, Long.parseLong(spec[1]));
				}
			}
		}
	}

	public void nextCycle(Node node, int protocolID) {
		IPeerSelector service = (IPeerSelector) node
				.getProtocol(fPeersamplingId);
		Node peer = service.selectPeer(node);
		if (peer == null) {
			return;
		}

		PeersamplingDebuggingApp app = (PeersamplingDebuggingApp) peer
				.getProtocol(protocolID);
		app.contact();
		fCounter.increment(peer);

		int max = Integer.MAX_VALUE;
		for (Node key : fCounter) {
			max = Math.min(max, fCounter.count(key));
		}

		fSeen = max;
	}

	public boolean isMonitored(Node node) {
		long id = node.getID();

		if (!fMonitor.contains(id)) {
			return false;
		}

		if (!fRounds.containsKey(id)) {
			return true;
		}

		return (fRounds.get(id) - 1) == CommonState.getTime();
	}

	public Map<Node, Integer> getAccesses() {
		return fCounter.asMap();
	}

	public void contact() {
		fContacted++;
	}

	public int getContacts() {
		return fContacted;
	}

	public int seen() {
		return fSeen;
	}

	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			PeersamplingDebuggingApp clone = (PeersamplingDebuggingApp) super
					.clone();
			clone.fCounter = (MultiCounter<Node>) fCounter.clone();

			return clone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException(ex);
		}
	}
}
