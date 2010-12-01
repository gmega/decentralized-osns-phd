package it.unitn.disi.graph;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import peersim.config.AutoConfig;

/**
 * Creates an undirected graph from a directed one. Note that this means, in our
 * case, ensuring that for each edge (i, j) there is <b>at least one</b> edge
 * from j to i. If the graph is not simple, this procedure <b>does not</b> guarantee
 * that the resulting graph will be "undirected".
 * 
 * @author giuliano
 */
@AutoConfig
public class Undirect implements ITransformer {

	public void execute(InputStream is, OutputStream oup) throws IOException {
		LightweightStaticGraph graph = LightweightStaticGraph
				.undirect(LightweightStaticGraph.load(new ByteGraphDecoder(is)));

		byte[] buf = new byte[4];
		for (int i = 0; i < graph.size(); i++) {
			for (int j : graph.getNeighbours(i)) {
				oup.write(CodecUtils.encode(i, buf));
				oup.write(CodecUtils.encode(j, buf));
			}
		}
	}
}
