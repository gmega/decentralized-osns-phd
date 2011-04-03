package it.unitn.disi.network.churn.tracebased;

import it.unitn.disi.network.GenericValueHolder;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.OrderingUtils;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.collections.ListExchanger;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Randomly assigns IDs from an ID pool (specified in an external file) to nodes
 * in the network. Supports filtering by country.
 * 
 * @author giuliano
 */
@AutoConfig
@StructuredLog(key = "RandomAssign", fields = { "avt_id", "peersim_id" })
public class RandomAssign implements Control {

	@Attribute("idfile")
	String fIdFile;

	@Attribute(value = "countries", defaultValue = "all")
	String fCountryList;

	@Attribute("trace_id")
	int fIdHolder;

	private final TableWriter fLog;

	public RandomAssign(
			@Attribute("TabularLogManager") TabularLogManager manager) {
		fLog = manager.get(RandomAssign.class);
	}

	@Override
	public boolean execute() {
		try {
			return this.execute0();
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	public boolean execute0() throws IOException {
		Set<String> countries = countries();
		TableReader reader = new TableReader(new FileInputStream(new File(
				fIdFile)));

		ArrayList<String> idPool = new ArrayList<String>();
		while (reader.hasNext()) {
			reader.next();
			if (countries != null && !countries.contains(reader.get("country"))) {
				continue;
			}
			idPool.add(reader.get("avt_id"));
		}

		if (idPool.size() < Network.size()) {
			throw new RuntimeException(
					"Not enough IDs in pool to cover the network ("
							+ idPool.size() + " < " + Network.size() + ").");
		}

		// Shuffles the ID array.
		OrderingUtils.permute(0, idPool.size(),
				new ListExchanger<String>().setList(idPool), CommonState.r);

		// Assigns sequentially.
		for (int i = 0; i < Network.size(); i++) {
			Node node = Network.get(i);
			String avtId = idPool.get(i);
			GenericValueHolder holder = (GenericValueHolder) node
					.getProtocol(fIdHolder);
			holder.setValue(avtId);

			fLog.set("avt_id", avtId);
			fLog.set("peersim_id", node.getID());
			fLog.emmitRow();
		}

		return false;
	}

	private Set<String> countries() {
		if (fCountryList.equals("all")) {
			return null;
		}
		HashSet<String> countries = new HashSet<String>();
		for (String country : fCountryList.split(",")) {
			countries.add(country);
		}
		return countries;
	}

}
