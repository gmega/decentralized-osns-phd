package it.unitn.disi.graph.cli;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;

/**
 * Transforms a graph by cutting off all vertices with out-degree less or equal
 * to a given threshold. A better version of this filter would discard strongly
 * connected components with size below a given threshold, but this is too
 * expensive to run on graphs of the sizes we're processing.
 * 
 * @author giuliano
 */
@AutoConfig
public class DegreeCutoff implements ITransformer {
	
	@Attribute("cutoff")
	private int fThreshold;

	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		LightweightStaticGraph graph = LightweightStaticGraph
				.load(new ByteGraphDecoder(is));
		BitSet excluded = new BitSet(graph.size());
		byte[] buf = new byte[4];		
		
		// Excludes vertices falling below the threshold.
		for (int i = 0; i < graph.size(); i++) {
			if(graph.degree(i) <= fThreshold) {
				excluded.set(i);
			}
		}
		
		// Rewrites the graph without the edges containing
		// excluded vertices.
		for (int i = 0; i < graph.size(); i++) {
			if (excluded.get(i)) {
				continue;
			}
			for (int neighbor : graph.fastGetNeighbours(i)) {
				if (excluded.get(neighbor)) {
					continue;
				}
				oup.write(CodecUtils.encode(i, buf));
				oup.write(CodecUtils.encode(neighbor, buf));
			}
		}
	}

}
