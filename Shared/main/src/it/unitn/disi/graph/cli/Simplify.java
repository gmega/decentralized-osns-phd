package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

import java.io.IOException;
import java.io.InputStream;
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
		ProgressTracker tracker = Progress.newTracker("simplifying", graph.size());
		tracker.startTask();
		boolean [] seen = new boolean[graph.size()];
		for (int i = 0; i < graph.size(); i++) {
			/** Ad-hoc optimization based on the assumption that
			 * low degree nodes are abundant.
			 */
			if (graph.degree(i) < 2) {
				continue;
			}
			
			// Scouts for duplicate edges and self-loops.
			seen[i] = true;
			int [] neighbors = graph.fastGetNeighbours(i);
			for (int j = 0; j < neighbors.length; j++) {
				int neighbor = neighbors[j];
				if (!seen[neighbor]) {
					oup.write(CodecUtils.encode(i, buf));
					oup.write(CodecUtils.encode(neighbor, buf));
					seen[j] = true;
				}
			}
			
			// Clears the set.
			seen[i] = false;
			for (int j = 0; j < neighbors.length; j++) {
				seen[neighbors[j]] = false;
			}
			
			tracker.tick();
		}
		
		tracker.done();
	}
}
