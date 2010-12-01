package it.unitn.disi.graph.cli;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ByteGraphInvert implements ITransformer {

	public void execute(InputStream is, OutputStream oup) throws IOException {
		BufferedOutputStream boup = new BufferedOutputStream(oup);
		ByteGraphDecoder dec = new ByteGraphDecoder(is);
		byte[] buf = new byte[Integer.SIZE/8];
		try {
			while (dec.hasNext()) {
				int source = dec.getSource();
				oup.write(CodecUtils.encode(dec.next(), buf));
				oup.write(CodecUtils.encode(source, buf));
			}
		} finally {
			if (boup != null) {
				boup.flush();
				boup.close();
			}
		}
	}

}
