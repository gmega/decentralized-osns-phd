package it.unitn.disi.cli;

import static it.unitn.disi.utils.logging.CodecUtils.encode;
import it.unitn.disi.codecs.GraphCodecHelper;
import it.unitn.disi.codecs.GraphDecoder;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class GraphRemap implements IMultiTransformer {
	
	static enum Inputs {
		GRAPH(0);
		
		public final int index;
		
		private Inputs(int index) {
			this.index = index;
		}
	}
	
	static enum Outputs {
		GRAPH(0), MAPFILE(1);
		
		public final int index;
		
		private Outputs(int index) {
			this.index = index;
		}
	}

	private String fDecoder;
	
	private Map<Integer, Integer> fMapped;

	private int fIds;

	private byte[] fBuf = new byte[4];

	public GraphRemap(@Attribute("decoder") String decoder) {
		fMapped = new HashMap<Integer, Integer>();
		fDecoder = decoder;
	}

	public void execute(InputStream [] input, OutputStream [] output)
			throws Exception {

		GraphDecoder dec = GraphCodecHelper.createDecoder(input[Inputs.GRAPH.index], fDecoder);
		OutputStream graph = output[Outputs.GRAPH.index]; 
		PrintStream mapfile = new PrintStream(output[Outputs.MAPFILE.index]);

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
