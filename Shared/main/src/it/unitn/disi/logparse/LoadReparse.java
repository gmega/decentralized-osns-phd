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
 * Computes residue, corrected residue, average uptime, and duplicate ratio from
 * load figures.
 * 
 * @author giuliano
 */
@AutoConfig
public class LoadReparse implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		HashMap<Long, Experiment> experiments = new HashMap<Long, Experiment>();
		TableReader reader = new TableReader(is);
		TableWriter writer = new TableWriter(new PrintStream(oup),
				new String[] { "root", "degree", "delivered", "root_uptime",
						"zero_uptime", "residue", "corrected_residue",
						"duplicates", "duplicate_ratio" });

		while (reader.hasNext()) {
			reader.next();
			long root = Long.parseLong(reader.get("root"));
			long id = Long.parseLong(reader.get("id"));
			Experiment exp = getCreate(experiments, root);
			exp.add(id, Integer.parseInt(reader.get("received")),
					Integer.parseInt(reader.get("uptime")));
		}
		
		for (Experiment exp : experiments.values()) {
			writer.set("root", exp.id);
			writer.set("degree", exp.friends());
			writer.set("delivered", exp.delivered());
			writer.set("root_uptime", exp.rootUptime());
			writer.set("zero_uptime", exp.zeroUptime());
			writer.set("residue", exp.residue());
			writer.set("corrected_residue", exp.correctedResidue());
			writer.set("duplicates", exp.duplicates());
			writer.set("duplicate_ratio", exp.duplicateRatio());
			writer.emmitRow();
		}
	}

	private Experiment getCreate(HashMap<Long, Experiment> experiments, long id) {
		Experiment exp = experiments.get(id);
		if (exp == null) {
			exp = new Experiment(id);
			experiments.put(id, exp);
		}
		return exp;
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
			return fDuplicates/(double)fParticipants;
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
