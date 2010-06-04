package it.unitn.disi.cli;

import it.unitn.disi.codecs.AdjListGraphDecoder;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Adj2ByteGraph implements ITransformer {

	public void execute(InputStream is, OutputStream oup) throws IOException {
		AdjListGraphDecoder dec = new AdjListGraphDecoder(is);
		byte [] buf = new byte[4];
		while (dec.hasNext()) {
			Integer source = dec.getSource();
			Integer target = dec.next();
			oup.write(CodecUtils.encode(source, buf));
			oup.write(CodecUtils.encode(target, buf));
		}
	}

}
