package it.unitn.disi.graph.cli;

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

/**
 * {@link GraphRemap} reassigns vertex IDs to a contiguous range.
 * 
 * @author giuliano
 */
@AutoConfig
public class GraphRemap implements IMultiTransformer {

	public static enum Inputs {
		GRAPH;
	}

	public static enum Outputs {
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

	public void execute(StreamProvider p) throws Exception {
		int edges = 0;
		GraphDecoder dec = GraphCodecHelper.createDecoder(
				p.input(Inputs.GRAPH), fDecoder);
		OutputStream graph = p.output(Outputs.GRAPH);
		PrintStream mapfile = new PrintStream(p.output(Outputs.MAPFILE));

		while (dec.hasNext()) {
			Integer source = dec.getSource();
			Integer target = dec.next();
			edges++;

			graph.write(encode(map(source, mapfile), fBuf));
			graph.write(encode(map(target, mapfile), fBuf));
		}

		System.err.println("Remapped " + fMapped.size() + " vertices, " + edges
				+ " edges.");
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
