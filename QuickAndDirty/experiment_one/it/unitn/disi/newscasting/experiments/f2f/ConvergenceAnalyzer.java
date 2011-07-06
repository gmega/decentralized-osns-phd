package it.unitn.disi.newscasting.experiments.f2f;

import it.unitn.disi.f2f.DiscoveryProtocol;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Tracks peer recovery protocol "convergence": descriptors for peers that are
 * alive but were not seen, and descriptors for peers that are dead, but are
 * still being seen.
 * 
 * @author giuliano
 */
@AutoConfig
@StructuredLog(key = "CONV", fields = { "time", "seen", "unseen", "stale",
		"phits", "dhits", "ahits", "dwaste" })
public class ConvergenceAnalyzer implements Control {

	private int fDiscoveryPid;

	private TableWriter fLog;

	public ConvergenceAnalyzer(@Attribute("discovery") int discoveryPid,
			@Attribute("TabularLogManager") TabularLogManager mgr) {
		fLog = mgr.get(ConvergenceAnalyzer.class);
		fDiscoveryPid = discoveryPid;
	}

	@Override
	public boolean execute() {

		int tot_discovered = 0; // Seen and alive.
		int tot_undiscovered = 0; // Unseen and alive.
		int tot_stale = 0; // Seen and not alive.
		
		int pshits = 0;
		int dchits = 0;
		int dcwaste = 0;
		int accidental = 0;

		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			if (!node.isUp()) {
				continue;
			}
			DiscoveryProtocol discovery = (DiscoveryProtocol) node
					.getProtocol(fDiscoveryPid);
			tot_discovered += discovery.seen();
			tot_undiscovered += discovery.unseen();
			tot_stale += discovery.stale();
			pshits += discovery.membershipHits();
			dchits += discovery.joinHits();
			dcwaste += discovery.joinWaste();
			accidental += discovery.accidentalHits();
		}
		
		fLog.set("time", CommonState.getTime());
		fLog.set("seen", tot_discovered);
		fLog.set("unseen", tot_undiscovered);
		fLog.set("stale", tot_stale);
		fLog.set("phits", pshits);
		fLog.set("dhits", dchits);
		fLog.set("dwaste", dcwaste);
		fLog.set("ahits", accidental);
		fLog.emmitRow();

		return false;
	}

}
