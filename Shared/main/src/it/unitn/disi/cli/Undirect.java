package it.unitn.disi.cli;

import it.unitn.disi.codecs.ByteGraphDecoder;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.CodecUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import peersim.graph.NeighbourListGraph;

public class Undirect implements ITransformer {

	public void execute(InputStream is, OutputStream oup) throws IOException {
		ByteGraphDecoder dec = new ByteGraphDecoder(is);
		NeighbourListGraph graph = MiscUtils.load(new NeighbourListGraph(false), dec);
		
		byte [] buf = new byte[4];
		for (int i = 0; i < graph.size(); i++) {
			for (int j : graph.getNeighbours(i)){
				oup.write(CodecUtils.encode(i, buf));
				oup.write(CodecUtils.encode(j, buf));
			}
		}
	}
}
