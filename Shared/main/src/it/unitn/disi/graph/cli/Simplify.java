package it.unitn.disi.graph.cli;

import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;

import java.io.IOException;
import java.io.OutputStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * {@link Simplify} removes duplicate edges from the input graph. Loads the
 * graph into memory.
 * 
 * @author giuliano
 */
@AutoConfig
public class Simplify extends GraphAnalyzer {

	public Simplify(@Attribute("decoder") String decoder) {
		super(decoder);
	}

	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream oup)
			throws IOException {
		byte[] buf = new byte[4];
		IProgressTracker tracker = Progress.newTracker("simplifying",
				graph.size());
		tracker.startTask();

		long dups = 0;
		long edges = 0;
		boolean[] seen = new boolean[graph.size()];
		for (int i = 0; i < graph.size(); i++) {
			// Scouts for duplicate edges and self-loops.
			seen[i] = true;
			int[] neighbors = graph.fastGetNeighbours(i);
			for (int j = 0; j < neighbors.length; j++) {
				int neighbor = neighbors[j];
				if (!seen[neighbor]) {
					oup.write(CodecUtils.encode(i, buf));
					oup.write(CodecUtils.encode(neighbor, buf));
					seen[neighbor] = true;
				} else {
					dups++;
				}
				edges++;
			}

			// Clears the set.
			seen[i] = false;
			for (int j = 0; j < neighbors.length; j++) {
				seen[neighbors[j]] = false;
			}

			tracker.tick();
		}

		tracker.done();

		System.err.println("Eliminated " + dups + " edges out of " + edges
				+ ".");
	}
}
