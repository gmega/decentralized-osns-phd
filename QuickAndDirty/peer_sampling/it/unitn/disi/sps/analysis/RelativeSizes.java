package it.unitn.disi.sps.analysis;

import java.io.PrintStream;

import peersim.config.AutoConfig;

import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.LightweightStaticGraph;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;

@AutoConfig
public class RelativeSizes implements IMultiTransformer {

	public static enum Inputs {
		onehop
	}

	public static enum Outputs {
		sizes
	}

	@Override
	public void execute(StreamProvider p) throws Exception {
		LightweightStaticGraph onehop = LightweightStaticGraph
				.load(new AdjListGraphDecoder(p.input(Inputs.onehop)));
		PrintStream ps = new PrintStream(p.output(Outputs.sizes));

		for (int i = 0; i < onehop.size(); i++) {
			int total = 0;
			for (int j = 0; j < onehop.degree(i); j++) {
				int neighbor = onehop.getNeighbor(i, j);
				total += onehop.degree(neighbor);
			}

			for (int j = 0; j < onehop.degree(i); j++) {
				int neighbor = onehop.getNeighbor(i, j);
				ps.println(i + " " + neighbor + " "
						+ (onehop.degree(neighbor) / ((double) total)));
			}
		}
	}
}
