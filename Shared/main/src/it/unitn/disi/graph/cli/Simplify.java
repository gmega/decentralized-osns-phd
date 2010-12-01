package it.unitn.disi.graph.cli;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.BitSet;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.logging.CodecUtils;

public class Simplify implements ITransformer {
	@Override
	public void execute(InputStream is, OutputStream oup) throws Exception {
		LightweightStaticGraph graph = LightweightStaticGraph.load(new ByteGraphDecoder(is));
		byte[] buf = new byte[4];
		BitSet seen = new BitSet(graph.size());
		for (int i = 0; i < graph.size(); i++) {
			seen.clear();
			seen.set(i);
			for (int j : graph.getNeighbours(i)) {
				if (!seen.get(j)) {
					continue;
				}
				oup.write(CodecUtils.encode(i, buf));
				oup.write(CodecUtils.encode(j, buf));
				seen.set(j);
			}
		}
	}
}
