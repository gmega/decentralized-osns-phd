package it.unitn.disi.graph;

import static it.unitn.disi.utils.logging.CodecUtils.encode;
import it.unitn.disi.cli.IMultiTransformer;
import it.unitn.disi.cli.StreamProvider;
import it.unitn.disi.graph.codecs.GraphCodecHelper;
import it.unitn.disi.graph.codecs.GraphDecoder;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class GraphRemap implements IMultiTransformer {
	
	static enum Inputs {
		GRAPH;
	}
	
	static enum Outputs {
		GRAPH, MAPFILE;
	}

	private String fDecoder;
	
	private Map<Integer, Integer> fMapped;

	private int fIds;

	private byte[] fBuf = new byte[4];

	public GraphRemap(@Attribute("decoder") String decoder) {
		fMapped = new HashMap<Integer, Integer>();
		fDecoder = decoder;
	}

	public void execute(StreamProvider p)
			throws Exception {

		GraphDecoder dec = GraphCodecHelper.createDecoder(p.input(Inputs.GRAPH), fDecoder);
		OutputStream graph = p.output(Outputs.GRAPH); 
		PrintStream mapfile = new PrintStream(p.output(Outputs.MAPFILE));

		while (dec.hasNext()) {
			Integer source = dec.getSource();
			Integer target = dec.next();

			graph.write(encode(map(source, mapfile), fBuf));
			graph.write(encode(map(target, mapfile), fBuf));
		}
	}

	private Integer map(Integer integer, PrintStream mapfile) {
		Integer mapped = fMapped.get(integer);
		if (mapped == null) {
			mapped = fIds++;
			fMapped.put(integer, mapped);
			mapfile.println(integer + " " + mapped);
		}

		return mapped;
	}
}
