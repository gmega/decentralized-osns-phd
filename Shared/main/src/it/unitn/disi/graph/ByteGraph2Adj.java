package it.unitn.disi.graph;

import it.unitn.disi.cli.ITransformer;
import it.unitn.disi.graph.codecs.ByteGraphDecoder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import peersim.config.AutoConfig;

@AutoConfig
public class ByteGraph2Adj implements ITransformer {
	public void execute(InputStream is, OutputStream oup) throws IOException {
		ByteGraphDecoder dec = new ByteGraphDecoder(is);
		Map<Integer, ArrayList<Integer>> adjacencies = new LinkedHashMap<Integer, ArrayList<Integer>>();

		while (dec.hasNext()) {
			Integer source = dec.getSource();
			Integer target = dec.next();

			ArrayList<Integer> neighbors = adjacencies.get(source);
			if (neighbors == null) {
				neighbors = new ArrayList<Integer>();
				adjacencies.put(source, neighbors);
			}

			neighbors.add(target);
		}

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(oup));
		try {
			for (Integer source : adjacencies.keySet()) {
				writer.write(source.toString());
				writer.write(" ");
				Iterator <Integer> it = adjacencies.get(source).iterator(); 
				while(it.hasNext()) {
					writer.write(it.next().toString());
					if (it.hasNext()) {
						writer.write(" ");
					}
				}
				
				writer.write("\n");
			}
		} finally {
			writer.close();
		}
	}
}
