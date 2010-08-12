package it.unitn.disi.cli;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import it.unitn.disi.codecs.GraphDecoder;
import it.unitn.disi.codecs.TextEdgeListDecoder;
import it.unitn.disi.utils.logging.CodecUtils;

public class ElText2Byte implements ITransformer {

	@Override
	public void execute(InputStream is, OutputStream oup) throws IOException {
		GraphDecoder dec = new TextEdgeListDecoder(is);

		byte[] buf = new byte[4];
		while (dec.hasNext()) {
			Integer source = dec.getSource();
			Integer target = dec.next();
			oup.write(CodecUtils.encode(source, buf));
			oup.write(CodecUtils.encode(target, buf));
		}
	}
}
