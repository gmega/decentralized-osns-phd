package it.unitn.disi.analysis.loadsim;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.tabular.ITableWriter;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Extracts experiment load summaries from per-round load logfiles.
 * 
 * @author giuliano
 */
@AutoConfig
public class Round2ExperimentLoad implements IMultiTransformer {

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
		ITableWriter writer = new TableWriter(out, new String[] { "root", "id",
				"sent", "received" });
		Map<Integer, UnitExperiment> exps = reader.load(false, true);

		for (UnitExperiment experiment : exps.values()) {
			Set<Integer> participants = experiment.participants();
			int lastRound = experiment.duration() - 1;
			for (Integer participant : participants) {
				writer.set("root", experiment.id());
				writer.set("id", participant);
				writer.set("sent",
						experiment.messagesSent(participant, lastRound));
				writer.set("received",
						experiment.messagesReceived(participant, lastRound));
				writer.emmitRow();
			}
		}
	}
}
