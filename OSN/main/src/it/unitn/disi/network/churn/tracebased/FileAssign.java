package it.unitn.disi.network.churn.tracebased;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import it.unitn.disi.network.GenericValueHolder;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.TableReader;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Reads AVT <-> PeerSim ID assignments from an external file.
 * 
 * @author giuliano
 */
@AutoConfig
public class FileAssign implements Control {

	@Attribute("idfile")
	String fIdFile;

	@Attribute("trace_id")
	int fIdHolder;

	@Override
	public boolean execute() {
		try {
			return this.execute0();
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	private boolean execute0() throws IOException {
		TableReader reader = new TableReader(new FileInputStream(new File(
				fIdFile)));

		Map<Long, String> fMapping = new HashMap<Long, String>();
		while (reader.hasNext()) {
			reader.next();
			fMapping.put(Long.parseLong(reader.get("node_id")),
					reader.get("avt_id"));
		}

		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			String avtId = fMapping.get(node.getID());
			if (avtId == null) {
				System.err.println("Error: node " + node.getID()
						+ " has no assigned AVT id.");
				return true;
			}
			GenericValueHolder holder = (GenericValueHolder) node
					.getProtocol(fIdHolder);
			holder.setValue(avtId);
		}

		return false;
	}

}
