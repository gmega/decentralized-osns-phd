package it.unitn.disi.sps.fake;

import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.NodeRegistry;

import java.io.File;
import java.io.FileInputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Node;

@AutoConfig
public class ReconstructionInitializer implements Control {
	
	// ----------------------------------------------------------------------
	// Table row names for input file.
	// ----------------------------------------------------------------------
	private static final String SOURCE = "source";
	private static final String DESTINATION = "destination";
	private static final String TIME = "time";
	
	// ----------------------------------------------------------------------

	@Attribute("file")
	private String fFile;
	
	@Attribute("protocol")
	private int fProtocolID;

	// ----------------------------------------------------------------------
	
	@Override
	public boolean execute() {
		try {
			return execute0();
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
	
	// ----------------------------------------------------------------------

	private boolean execute0() throws Exception {
		TableReader reader = new TableReader(new FileInputStream(
				new File(fFile)));

		INodeRegistry nr = NodeRegistry.getInstance();
		
		while(reader.hasNext()) {
			long source = Long.parseLong(reader.get(SOURCE));
			long target = Long.parseLong(reader.get(DESTINATION));
			int time = Integer.parseInt(reader.get(TIME));
			
			Node sourceNode = nr.getNode(source);
			Node targetNode = nr.getNode(target);
			
			FakeReconstructedNeighborhood fake = (FakeReconstructedNeighborhood) sourceNode.getProtocol(fProtocolID);
			fake.setReconstructionTime(targetNode, time);
		}
		
		return false;
	}
}
