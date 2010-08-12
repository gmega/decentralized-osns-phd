package it.unitn.disi.cli;

import it.unitn.disi.CommandAlias;
import it.unitn.disi.codecs.ByteGraphDecoder;
import it.unitn.disi.utils.ConfigurationProperties;
import it.unitn.disi.utils.ConfigurationUtils;
import it.unitn.disi.utils.InjectableProperty;
import it.unitn.disi.utils.InjectableProperty.Type;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Command-line utility for simplifying a graph. Assumes binary format. Written
 * in Java for scalability - it can process very large graphs.
 * 
 * @author giuliano
 */
@CommandAlias("simplify")
public class Simplify implements ITransformer, IParametricTransformer {
	
	public static final InjectableProperty DIRECTED = new InjectableProperty("directed",
			"fDirected", Type.BOOLEAN);
	
	public static final InjectableProperty DISCARD_INTERVAL = new InjectableProperty("discard_interval",
			"fDiscard", Type.INTEGER, "-1");
	
	private boolean fDirected;
	
	private int fDiscard;

	public void execute(InputStream is, OutputStream oup) throws IOException {
		ByteGraphDecoder dec = new ByteGraphDecoder(is);
		Set<Edge> edges = new HashSet<Edge>();
		
		int counter = 0;
		int current = -1;
		
		byte [] buf = new byte[4];
		while (dec.hasNext()) {
			Edge edge = new Edge(dec.getSource(), dec.next(), !fDirected);
			
			/** If we're trying to discard...**/
			if (fDiscard > 0){
				/** ... then the IDs must be ordered. Which means that the 
				 * current source can be either equal or one larger than "current",
				 * but never smaller. */
				if (edge.source > current) {
					current = edge.source;
				} else if (edge.source != current){
					throw new IllegalStateException("IDs are not ordered ("
							+ edge.source + "," + current
							+ "), cannot use reduced footprint mode.");
				}
			}
			
			if (!edges.contains(edge)) {
				edges.add(edge);
				oup.write(CodecUtils.encode(edge.source, buf));
				oup.write(CodecUtils.encode(edge.target, buf));
			}
			
			if (++counter == fDiscard) {
				/** Cleans up all edges involving nodes smaller than the current. */
				Iterator<Edge> it = edges.iterator();
				while(it.hasNext()) {
					Edge maybeStale = it.next();
					if (maybeStale.source < current && maybeStale.target < current) {
						it.remove();
					}
				}
			}
		}
	}

	public Set<String> required() {
		return ConfigurationUtils.collect(this.getClass());
	}

	public void setParameters(ConfigurationProperties props) {
		ConfigurationUtils.inject(this, props);
	}
	
}
