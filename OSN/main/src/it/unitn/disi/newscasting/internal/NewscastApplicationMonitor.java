package it.unitn.disi.newscasting.internal;

import static it.unitn.disi.utils.peersim.PeersimUtils.print;
import it.unitn.disi.SimulationEvents;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.StreamManager;

import java.io.IOException;
import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.IResolver;
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
	
	private EventCodec fCodec = new EventCodec(Byte.class, SimulationEvents.values());
	
	private OutputStream fLog;
	
	// --------------------------------------------------------------------------
	
	public NewscastApplicationMonitor(
			@Attribute(Attribute.AUTO) IResolver resolver,
			@Attribute(Attribute.AUTO) String prefix,
			@Attribute("StreamManager") StreamManager manager) throws IOException {
		fLog = manager.get(resolver, prefix);
	}

	public boolean execute() {
		int size = Network.size();
		
		IncrementalStats aeLists = new IncrementalStats();
		IncrementalStats aeElements = new IncrementalStats();
		
		int drift = 0;
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			
			if (!node.isUp()) {
				continue;
			}
			
			ICoreInterface app = (ICoreInterface) node.getProtocol(fAppId);
			drift += app.pendingReceives();
			aeLists.add(app.storage().distinctNodes());
			aeElements.add(app.storage().elements());
						
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
		
		try {
			fLog.write(fBuffer, 0, len);
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		
		return false;
	}

}

