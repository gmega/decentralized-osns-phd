package it.unitn.disi.logparse;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;

import peersim.config.AutoConfig;
import peersim.util.IncrementalStats;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.logparse.LoadReparse.Experiment;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableReader;
import it.unitn.disi.utils.tabular.TableWriter;

/**
 * Compute fairness from load figures.
 * 
 * @author giuliano
 */
@AutoConfig
public class SRFairness implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		TableReader reader = new TableReader(is);
		ITableWriter writer = new TableWriter(new PrintStream(oup),
				new String[] { "root", "send_fairness", "recv_fairness",
						"tot_fairness" });
		
		HashMap<Long, Experiment> experiments = new HashMap<Long, Experiment>();
		
		while (reader.hasNext()) {
			reader.next();			
			long root = Long.parseLong(reader.get("root"));
			int sent = Integer.parseInt(reader.get("sent"));
			int recv = Integer.parseInt(reader.get("received"));
			Experiment experiment = experiments.get(root);
			if (experiment == null) {
				experiment = new Experiment();
				experiments.put(root, experiment);
			}
			experiment.add(sent, recv);
		}
		
		for (Long key : experiments.keySet()) {
			writer.set("root", key);
			Experiment exp = experiments.get(key);
			writer.set("send_fairness", exp.sendFairness());
			writer.set("recv_fairness", exp.recvFairness());
			writer.set("tot_fairness", exp.totFairness());
			writer.emmitRow();
		}
	}

	class Experiment {
		private final IncrementalStats fSent = new IncrementalStats();

		private final IncrementalStats fRecv = new IncrementalStats();

		private final IncrementalStats fTot = new IncrementalStats();

		public void add(int sent, int recv) {
			fSent.add(sent);
			fRecv.add(recv);
			fTot.add(sent + recv);
		}

		public double sendFairness() {
			return (CV(fSent));
		}

		public double recvFairness() {
			return (CV(fRecv));
		}

		public double totFairness() {
			return (CV(fTot));
		}

		private double CV(IncrementalStats stats) {
			double avg = stats.getAverage();
			double std = stats.getStD();

			if (avg == 0.0 && std == 0.0) {
				return 1.0;
			}

			return std / avg;
		}
	}
}
