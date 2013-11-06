package it.unitn.disi.logparse;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.AutoConfig;

/**
 * Computes residue, corrected residue, average uptime, and duplicate ratio from
 * load figures.
 * 
 * @author giuliano
 */
@AutoConfig
public class LoadReparse implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader reader = new TableReader(is);
		ITableWriter writer = new TableWriter(new PrintStream(oup),
				new String[] { "root", "degree", "delivered", "root_uptime",
						"zero_uptime", "residue", "corrected_residue",
						"duplicates", "copies", "duplicate_ratio"});

		Experiment current = null;
		while (reader.hasNext()) {
			reader.next();
			long root = Long.parseLong(reader.get("root"));
			current = experiment(root, current, writer);
			long id = Long.parseLong(reader.get("id"));
			// Some older datasets don't have the uptime.
			String uptimeStr = reader.get("uptime");
			int uptime = 1;
			if (uptimeStr != null) {
				uptime = Integer.parseInt(uptimeStr);
			}

			current.add(id, Integer.parseInt(reader.get("received")), uptime);
		}
	}

	private Experiment experiment(long root, Experiment exp, ITableWriter writer) {
		Experiment ret = exp;
		if (exp == null) {
			ret = new Experiment(root);
		} else if (exp.id != root) {
			writer.set("root", exp.id);
			writer.set("degree", exp.friends());
			writer.set("delivered", exp.delivered());
			writer.set("root_uptime", exp.rootUptime());
			writer.set("zero_uptime", exp.zeroUptime());
			writer.set("residue", exp.residue());
			writer.set("corrected_residue", exp.correctedResidue());
			writer.set("duplicates", exp.duplicates());
			writer.set("duplicate_ratio", exp.duplicateRatio());
			writer.set("copies", exp.delivered() + exp.duplicates());
			writer.emmitRow();
			ret = new Experiment(root);
		}
		return ret;
	}

	static class Experiment {
		public final long id;

		private int fDelivered;

		private int fParticipants;

		private int fDuplicates;

		private int fUptime;

		private int fZeroUptime;

		private int fRootUptime;

		public Experiment(long id) {
			this.id = id;
		}

		public void add(long id, int received, int uptime) {
			fParticipants++;

			if (id == this.id) {
				fRootUptime = uptime;
			}

			if (received != 0) {
				if (id != this.id) {
					fDelivered++;
				}
				fDuplicates += (received - 1);
			}

			if (uptime == 0) {
				fZeroUptime++;
			}

			fUptime += uptime;
		}

		public int friends() {
			return fParticipants - 1;
		}

		public int delivered() {
			return fDelivered;
		}

		public int zeroUptime() {
			return fZeroUptime;
		}

		public int duplicates() {
			return fDuplicates;
		}

		public double duplicateRatio() {
			return fDuplicates / (double) fParticipants;
		}

		public double residue() {
			return residue0(friends(), fDelivered);
		}

		public double correctedResidue() {
			return residue0(correctedParticipants(), fDelivered);
		}

		public double avgUptime() {
			return fUptime / ((double) fParticipants);
		}

		public int rootUptime() {
			return fRootUptime;
		}

		public int correctedParticipants() {
			return friends() - fZeroUptime;
		}

		private double residue0(double participants, double delivered) {
			if (participants == 0.0 && delivered == 0.0) {
				return 0.0;
			}
			return 1.0 - (delivered / participants);
		}
	}
}
