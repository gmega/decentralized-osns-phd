package it.unitn.disi.cli;

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
import java.util.Set;

public class Simplify implements ITransformer, IParametricTransformer {
	
	public static final InjectableProperty DIRECTED = new InjectableProperty("directed",
			"fDirected", Type.BOOLEAN);
	
	private boolean fDirected = true;

	public void execute(InputStream is, OutputStream oup) throws IOException {
		ByteGraphDecoder dec = new ByteGraphDecoder(is);
		Set<Edge> edges = new HashSet<Edge>();
		
		byte [] buf = new byte[4];
		while (dec.hasNext()) {
			Edge edge = new Edge(dec.getSource(), dec.next(), !fDirected);
			if (!edges.contains(edge)) {
				System.err.println("Add edge: " + edge);
				edges.add(edge);
				oup.write(CodecUtils.encode(edge.source, buf));
				oup.write(CodecUtils.encode(edge.target, buf));
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
