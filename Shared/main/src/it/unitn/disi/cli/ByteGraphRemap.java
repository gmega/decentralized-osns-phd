package it.unitn.disi.cli;


import static it.unitn.disi.utils.logging.CodecUtils.encode;
import it.unitn.disi.codecs.ByteGraphDecoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ByteGraphRemap implements ITransformer {
	
	private Map<Integer, Integer> fMapped;

	private int fIds;
	
	private byte [] fBuf = new byte[4];

	public void execute(InputStream input, OutputStream output)
			throws IOException {

		init();

		ByteGraphDecoder dec = new ByteGraphDecoder(input);

		while (dec.hasNext()) {
			Integer source = dec.getSource();
			Integer target = dec.next();

			output.write(encode(map(source), fBuf));
			output.write(encode(map(target), fBuf));
		}
	}
	
	private Integer map(Integer integer) {
		Integer mapped = fMapped.get(integer);
		if (mapped == null) {
			mapped = fIds++;
			fMapped.put(integer, mapped);
		}
		
		return mapped;
	}

	private void init() {
		fIds = 0;
		fMapped = new HashMap<Integer, Integer>();
	}

}
