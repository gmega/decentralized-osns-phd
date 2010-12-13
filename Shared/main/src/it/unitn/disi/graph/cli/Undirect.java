package it.unitn.disi.graph.cli;

import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.OutputStream;

import peersim.config.Attribute;
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
public class Undirect extends GraphAnalyzer {
	
	public Undirect(@Attribute("decoder") String decoder) {
		super(decoder);
	}
	
	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream oup) throws IOException{
		
		byte[] buf = new byte[4];
		for (int i = 0; i < graph.size(); i++) {
			for (int j : graph.getNeighbours(i)) {
				oup.write(CodecUtils.encode(i, buf));
				oup.write(CodecUtils.encode(j, buf));
				if (!graph.isEdge(j, i)) {
					oup.write(CodecUtils.encode(j, buf));
					oup.write(CodecUtils.encode(i, buf));
				}
			}
		}
	}
}
