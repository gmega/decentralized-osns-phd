package it.unitn.disi.network;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Determines the ID space of a trace network, and then randomly assigns these
 * IDs to the underlying network.
 * 
 * @author giuliano
 */
@AutoConfig
public class RandomInitializer implements Control{

	@Attribute("traceid_holder")
	private int fIdHolder;
	
	@Attribute(Attribute.AUTO)
	private String tracefile;
	
	public RandomInitializer() { }

	@Override
	public boolean execute() {
		System.err.print("Identifying tracefile IDs...");
		Set<String> ids = new HashSet<String>();
		
		EvtDecoder decoder;
		try {
			decoder = new EvtDecoder(new FileReader(new File(tracefile)));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
				
		while (decoder.hasNext()) {
			TraceEvent evt = decoder.next();
			ids.add(evt.nodeId);
		}
		
		if (ids.size() == 0) {
			System.err.println("error!");
			System.err.println("Trace network has zero elements.");
			return true;
		}
		
		System.err.println("[" + ids.size() + "] ids found.");
		Iterator<String> it = ids.iterator();
		int size = Network.size();
		for (int i = 0; i < size; i++) {
			Node node = Network.get(i);
			GenericValueHolder gvh = (GenericValueHolder) node
					.getProtocol(fIdHolder);
			
			if (!it.hasNext()) {
				it = ids.iterator();
			}
			
			gvh.setValue(it.next());
		}
		
		return false;
	}
}
