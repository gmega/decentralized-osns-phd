package it.unitn.disi.application;

import static it.unitn.disi.utils.peersim.PeersimUtils.print;
import it.unitn.disi.SimulationEvents;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.LogManager;

import java.io.IOException;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.util.IncrementalStats;

@AutoConfig
public class NewscastApplicationMonitor implements Control {
	
	// --------------------------------------------------------------------------
	// Parameters.
	// --------------------------------------------------------------------------
	@Attribute("application")
	private int fAppId;
	
	@Attribute("resetcounters")
	private boolean fResetCounters;
	
	// --------------------------------------------------------------------------
	// Log and event handling.
	// --------------------------------------------------------------------------
	
	private static final byte [] fBuffer = new byte[Long.SIZE/Byte.SIZE + 1];
	
	private LogManager fManager = LogManager.getInstance();
	
	private EventCodec fCodec = new EventCodec(Byte.class, SimulationEvents.values());
	
	private String fLogName;
	
	// --------------------------------------------------------------------------
	
	public NewscastApplicationMonitor(@Attribute(Attribute.PREFIX) String prefix) throws IOException {
		fLogName = fManager.add(prefix).get(0);
	}

	public boolean execute() {
		int size = Network.size();
		
		IncrementalStats aeLists = new IncrementalStats();
		IncrementalStats aeElements = new IncrementalStats();
		//IncrementalStats rumorListSize = new IncrementalStats();
		
		int drift = 0;
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			
			if (!node.isUp()) {
				continue;
			}
			
			NewscastApplication app = (NewscastApplication) node.getProtocol(fAppId);
			drift += app.pendingReceives();
			aeLists.add(app.getStorage().lists());
			aeElements.add(app.getStorage().elements());
						
			if (fResetCounters) {
				app.resetCounters();
			}
		}
		
		//print("rumor_list_size", rumorListSize);
		print("anti_entropy_lists", aeLists);
		print("anti_entropy_list_size", aeElements);

		if (drift != 0) {
			System.out.println("drift: " + drift);
		}
		
		// Logs round end.
		int len = fCodec.encodeEvent(fBuffer, 0,
				SimulationEvents.ROUND_ENDED.magicNumber(),
				CommonState.getTime());
		fManager.logWrite(fLogName, fBuffer, len);
		
		if(CommonState.getTime() == (CommonState.getEndTime() - 1)) {
			System.out.println("DUMPCONTACTS:");
			
			for (int i = 0; i < Network.size(); i++) {
				Node node = Network.get(i);
				NewscastApplication app = (NewscastApplication) node.getProtocol(fAppId);
				System.out.println(node.getID() + " " + app.contacts());
			}
		
			System.out.println("ENDDUMPCONTACTS:");
		}
		
		return false;
	}

}

