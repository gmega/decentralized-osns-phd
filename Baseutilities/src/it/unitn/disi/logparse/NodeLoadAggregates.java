package it.unitn.disi.logparse;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import peersim.config.AutoConfig;

/**
 * Computes average and aggregate loads.
 * 
 * @author giuliano
 */
@AutoConfig
public class NodeLoadAggregates implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		HashMap<Long, NodeData> nodes = new HashMap<Long, NodeData>();
		TableReader reader = new TableReader(is);
		ITableWriter writer = new TableWriter(new PrintStream(oup),
				new String[] { "id", "posts", "sent", "sent_as_root",
						"received", "total", "disseminated", "updates",
						"dups_generated", "dups_received", "experiments" });

		boolean printWarning = true;

		while (reader.hasNext()) {
			reader.next();
			long id = Long.parseLong(reader.get("id"));
			long root = Long.parseLong(reader.get("root"));
			NodeData exp = getCreate(nodes, id);

			// Compatibility with older datasets.
			int dupsSent = 0;
			try {
				dupsSent = Integer.parseInt(reader.get("dups_sent"));
			} catch (Exception ex) {
				if (printWarning) {
					System.err.println("Compatibility mode on.");
					printWarning = false;
				}
			}

			int sent = Integer.parseInt(reader.get("sent"));
			int recv = Integer.parseInt(reader.get("received"));
			if (id == root) {
				exp.addAsRoot(sent, dupsSent);
			} else {
				exp.add(sent, recv, dupsSent);
			}
		}

		for (Long id : nodes.keySet()) {
			NodeData data = nodes.get(id);
			writer.set("id", id);
			writer.set("posts", data.posts());
			writer.set("sent", data.sent());
			writer.set("sent_as_root", data.sentAsRoot());
			writer.set("received", data.received());
			writer.set("total", data.sent() + data.received());
			writer.set("disseminated", data.disseminated());
			writer.set("updates", data.updates());
			writer.set("dups_generated", data.duplicatesSent());
			writer.set("dups_received", data.duplicatesReceived());
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

		private int fSentAsRoot;

		private int fDupsAsRoot;

		private int fWasRoot;

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

		public void addAsRoot(int sent, int duplicatesSent) {
			add(sent, 0, duplicatesSent);
			fSentAsRoot += sent;
			fDupsAsRoot += duplicatesSent;
			fWasRoot++;
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

		public int sentAsRoot() {
			return fSentAsRoot;
		}

		public int duplicatesAsRoot() {
			return fDupsAsRoot;
		}

		public int posts() {
			return fWasRoot;
		}
	}
}
