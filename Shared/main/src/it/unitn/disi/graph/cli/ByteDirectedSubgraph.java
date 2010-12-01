package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Extracts the undirected core out of a directed subgraph.
 * 
 * @author giuliano
 */
public class ByteDirectedSubgraph implements ITransformer{

	public void execute(InputStream is, OutputStream oup) throws IOException {
		LightweightStaticGraph g = LightweightStaticGraph.load(new ByteGraphDecoder(is));
		
		byte [] buf = new byte[4];
		
		for (int i = 0; i < g.size(); i++) {
			for (int neighbor : g.fastGetNeighbours(i)) {
				int [] neighborNeighbors = g.fastGetNeighbours(neighbor);
				int idx = Arrays.binarySearch(neighborNeighbors, i);
				if (idx >= 0 && idx < neighborNeighbors.length && neighborNeighbors[idx] == i) {
					oup.write(CodecUtils.encode(i, buf));
					oup.write(CodecUtils.encode(neighbor, buf));
				}
			}
		}
	}

}
