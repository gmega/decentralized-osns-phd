package it.unitn.disi.graph.cli;

import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.TarjanState;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.tabular.TableWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class ComputeConnectivity extends GraphAnalyzer {

	public ComputeConnectivity(@Attribute("decoder") String decoder) {
		super(decoder);
	}

	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream oStream)
			throws IOException {

		PrintStream out = new PrintStream(oStream);

		System.err.println("Running Tarjan's algorithm.");
		try {
			TableWriter writer = new TableWriter(new PrintStream(oStream),
					"id", "components", "degree");

			TarjanState state = new TarjanState();
			
			for (int i = 0; i < graph.size(); i++) {
				LightweightStaticGraph egonet = LightweightStaticGraph
						.subgraph(graph, graph.fastGetNeighbours(i));
				
				state.ensureSize(egonet.size());
				writer.set("id", i);
				writer.set("components", GraphAlgorithms.tarjan(state, egonet));
				writer.set("degree", egonet.size());
				writer.emmitRow();
			}
		} finally {
			out.close();
		}
	}
}
