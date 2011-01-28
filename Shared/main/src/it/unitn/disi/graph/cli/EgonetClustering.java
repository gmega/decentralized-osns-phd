package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

import java.io.PrintStream;

import peersim.config.AutoConfig;

@AutoConfig
public class EgonetClustering implements IMultiTransformer {

	public static enum Inputs {
		GRAPH;
	}

	public static enum Outputs {
		CLUSTERINGS;
	}

	@Override
	public void execute(StreamProvider p) throws Exception {
		LightweightStaticGraph graph = LightweightStaticGraph
				.load(new ByteGraphDecoder(p.input(Inputs.GRAPH)));
		
		PrintStream out = new PrintStream(p.output(Outputs.CLUSTERINGS));
		out.println("id degree triads possible_triads coefficient");
		
		ProgressTracker tracker = Progress.newTracker("computing clustering", graph.size());
		tracker.startTask();
		for (int i = 0; i < graph.size(); i++) {
			StringBuffer buffer = new StringBuffer();
			int n = graph.degree(i);
			int triads = GraphAlgorithms.countTriads(graph, i);
			double clique_triads = (n * (n - 1))/2.0; 
			buffer.append(i);
			buffer.append(" ");
			buffer.append(n);
			buffer.append(" ");
			buffer.append(triads);
			buffer.append(" ");
			buffer.append(clique_triads);
			buffer.append(" ");
			buffer.append(triads/clique_triads);
			out.println(buffer);
			tracker.tick();
		}
		tracker.done();
	}

}
