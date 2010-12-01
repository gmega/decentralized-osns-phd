package it.unitn.disi.graph;

import it.unitn.disi.CommandAlias;
import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

/**
 * Command-line utility for simplifying a graph. Assumes binary format. Written
 * in Java for scalability - it can process very large graphs.
 * 
 * @author giuliano
 */
@CommandAlias("simplify")
@AutoConfig
public class Simplify implements ITransformer {
	
	@Attribute("directed")
	private boolean fDirected;

	public void execute(InputStream is, OutputStream oup) throws IOException {
		ByteGraphDecoder dec = new ByteGraphDecoder(is);
		Set<Edge> edges = new HashSet<Edge>();
		
		int current = -1;
		
		byte [] buf = new byte[4];
		while (dec.hasNext()) {
			Edge edge = new Edge(dec.getSource(), dec.next(), !fDirected);
			
			/** 
			 * Source IDs must be ordered, otherwise we cannot do this 
			 * without loading the whole graph into memory.
			 */
			if (edge.source > current) {
				current = edge.source;
				edges.clear();
			} else if (edge.source != current){
				throw new IllegalStateException("IDs are not ordered ("
						+ edge.source + "," + current
						+ "), cannot use reduced footprint mode.");
			}
			
			if (!edges.contains(edge)) {
				edges.add(edge);
				oup.write(CodecUtils.encode(edge.source, buf));
				oup.write(CodecUtils.encode(edge.target, buf));
			}
		}
	}
}
