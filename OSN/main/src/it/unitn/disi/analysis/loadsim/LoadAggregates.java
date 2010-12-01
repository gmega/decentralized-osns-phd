package it.unitn.disi.analysis.loadsim;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

@AutoConfig
public class LoadAggregates implements IMultiTransformer {

	public static enum Inputs {
		graph, experiments;
	}

	public static enum Outputs {
		statistics;
	}

	@Attribute("decoder")
	private String fDecoder;

	@Override
	public void execute(StreamProvider p) throws Exception {
		IndexedNeighborGraph g = LightweightStaticGraph.load(GraphCodecHelper
				.uncheckedCreateDecoder(p.input(Inputs.graph), fDecoder));
		UnitExperimentReader reader = new UnitExperimentReader(
				p.input(Inputs.experiments), g);

		PrintStream out = new PrintStream(p.output(Outputs.statistics));
		out.println("id degree sent received points");
		
		Map<Integer, UnitExperiment> exps = reader.load();
		Map<Integer, Statistics> total = new HashMap<Integer, Statistics>();
		
		for (UnitExperiment experiment : exps.values()) {
			Set<Integer> participants = experiment.participants();
			int lastRound = experiment.duration() - 1;
			
			for (Integer participant : participants) {
				Statistics stats = getCreate(total, participant, g.degree(participant));
				for (int i = 0; i <= lastRound; i++) {
					stats.sent += experiment.messagesSent(participant, i);
					stats.received += experiment.messagesReceived(participant, i);
				}
				stats.n++;
			}
		}
		
		for (Integer id : total.keySet()) {
			Statistics stat = total.get(id);
			StringBuffer buffer = new StringBuffer();
			buffer.append(id);
			buffer.append(" ");
			buffer.append(g.degree(id));
			buffer.append(" ");
			buffer.append(stat.sent);
			buffer.append(" ");
			buffer.append(stat.received);
			buffer.append(" ");
			buffer.append(stat.n);
			out.println(buffer.toString());
		}
	}

	private Statistics getCreate(Map<Integer, Statistics> total,
			Integer participant, int degree) {
		Statistics stat = total.get(participant);
		if (stat == null) {
			stat = new Statistics(participant, degree);
			total.put(participant, stat);
		}
		
		return stat;
	}
}

class Statistics {
	
	public Statistics(int id, int degree) {
		this.id = id;
		this.degree = degree;
	}
	
	int id;
	int degree;
	int sent;
	int received;
	int n;
}
