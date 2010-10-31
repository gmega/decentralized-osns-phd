package it.unitn.disi.network;

import it.unitn.disi.SimulationEvents;
import it.unitn.disi.utils.logging.EventCodec;
import it.unitn.disi.utils.logging.LogManager;

import java.io.IOException;

import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Simple initializer which brings the entire network down.
 * 
 * @author giuliano
 */
public class AllDown implements Control {
	
	// ------------------------------------------------------------------
	// Event encoding/decoding.
	// ------------------------------------------------------------------
	
	private static final EventCodec fCodec = new EventCodec(Byte.class,
			SimulationEvents.values());

	protected static final byte[] fBuffer = new byte[SimulationEvents.set
			.sizeof(SimulationEvents.set.getLargest())];
	
	private LogManager fLogManager;
	
	private final String fLogId;
	
	public AllDown(String s) throws IOException{ 
		fLogManager = LogManager.getInstance();
		fLogId = fLogManager.addUnique(s);
	}
	
	@Override
	public boolean execute() {
		int size = Network.size();
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			node.setFailState(Node.DOWN);
			fCodec.encodeEvent(fBuffer, 0, 
					SimulationEvents.NODE_DEPART.magicNumber(), 
					node.getID(), 
					CommonState.getTime());
			try {
				fLogManager.get(fLogId).write(fBuffer);
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
		
		return false;
	}

}
