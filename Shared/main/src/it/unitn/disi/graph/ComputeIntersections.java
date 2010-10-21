package it.unitn.disi.graph;

import java.io.PrintStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.cli.IMultiTransformer;

import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.codecs.GraphCodecHelper;

@AutoConfig
public class ComputeIntersections implements IMultiTransformer {

	public static enum Inputs {
		ONE_HOP, TWO_HOP;
	}

	public static enum Outputs {
		INTERSECTIONS;
	}

	private String fDecoder;

	public ComputeIntersections(@Attribute("decoder") String decoder) {
		fDecoder = decoder;
	}

	@Override
	public void execute(StreamProvider p) throws Exception {
		LightweightStaticGraph onehop = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(p.input(Inputs.ONE_HOP),
						fDecoder));
		LightweightStaticGraph twohop = LightweightStaticGraph
				.load(GraphCodecHelper.createDecoder(p.input(Inputs.TWO_HOP),
						fDecoder));
		PrintStream ps = new PrintStream(p.output(Outputs.INTERSECTIONS));

		for (int i = 0; i < onehop.size(); i++) {
			for (int j = 0; j < onehop.degree(i); j++) {
				int neighbor = onehop.getNeighbor(i, j);
				ps.println(i + " " + neighbor + " "
						+ this.countIntersections(twohop, i, neighbor));
			}
		}
	}

	private int countIntersections(LightweightStaticGraph graph, int v1, int v2) {
		int u, v;
		if (graph.degree(v1) > graph.degree(v2)) {
			u = v2;
			v = v1;
		} else {
			u = v1;
			v = v2;
		}

		int count = 0;
		for (int i = 0; i < graph.degree(u); i++) {
			int neighbor = graph.getNeighbor(u, i);
			if (graph.isEdge(v, neighbor)) {
				count++;
			}
		}
		return count;
	}

}
