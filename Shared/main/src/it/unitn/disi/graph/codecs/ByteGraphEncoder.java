package it.unitn.disi.graph.codecs;

import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.OutputStream;

public class ByteGraphEncoder extends AbstractEdgeListEncoder {

	private final byte[] fBuffer = new byte[(Integer.SIZE / Byte.SIZE)];

	public ByteGraphEncoder(OutputStream stream) {
		super(stream);
	}

	@Override
	protected void encodePair(int source, int target, OutputStream stream)
			throws IOException {
		stream.write(CodecUtils.encode(source, fBuffer, 0));
		stream.write(CodecUtils.encode(target, fBuffer, 0));
	}

}
