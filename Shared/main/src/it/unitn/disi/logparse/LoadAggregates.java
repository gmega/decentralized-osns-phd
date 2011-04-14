package it.unitn.disi.logparse;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.TableReader;
import it.unitn.disi.utils.TableWriter;

/**
 * Computes average and aggregate loads.
 * 
 * @author giuliano
 */
@AutoConfig
public class LoadAggregates implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		HashMap<Long, NodeData> nodes = new HashMap<Long, NodeData>();
		TableReader reader = new TableReader(is);
		TableWriter writer = new TableWriter(new PrintStream(oup),
				new String[] { "id", "sent", "received", "total",
						"disseminated", "updates", "dups_generated",
						"dups_received", "experiments" });

		while (reader.hasNext()) {
			reader.next();
			long id = Long.parseLong(reader.get("id"));
			NodeData exp = getCreate(nodes, id);
			exp.add(Integer.parseInt(reader.get("sent")),
					Integer.parseInt(reader.get("received")),
					Integer.parseInt(reader.get("dups_sent")));
		}

		for (Long id : nodes.keySet()) {
			NodeData data = nodes.get(id);
			writer.set("id", id);
			writer.set("sent", data.sent());
			writer.set("received", data.received());
			writer.set("total", data.sent() + data.received());
			writer.set("dups_generated", data.duplicatesSent());
			writer.set("dups_received", data.duplicatesReceived());
			writer.set("disseminated", data.disseminated());
			writer.set("updates", data.updates());
			writer.set("experiments", data.experiments());
			writer.emmitRow();
		}
	}

	private NodeData getCreate(HashMap<Long, NodeData> nodes, long id) {
		NodeData exp = nodes.get(id);
		if (exp == null) {
			exp = new NodeData();
			nodes.put(id, exp);
		}
		return exp;
	}

	class NodeData {

		private int fExperiments;

		private int fSent;

		private int fReceived;

		private int fUpdates;

		private int fDuplicatesReceived;

		private int fDuplicatesSent;

		public NodeData() {
		}

		public void add(int sent, int received, int duplicatesSent) {
			fSent += sent;
			fReceived += received;
			fDuplicatesSent += duplicatesSent;
			if (received > 0) {
				fUpdates++;
				fDuplicatesReceived += (received - 1);
			}

			fExperiments++;
		}

		public int sent() {
			return fSent;
		}

		public int received() {
			return fReceived;
		}

		public int updates() {
			return fUpdates;
		}

		public int duplicatesSent() {
			return fDuplicatesSent;
		}

		public int duplicatesReceived() {
			return fDuplicatesReceived;
		}

		public int disseminated() {
			return fSent - fDuplicatesSent;
		}

		public int experiments() {
			return fExperiments;
		}
	}
}
