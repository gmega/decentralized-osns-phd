package it.unitn.disi.churn.config;

import it.unitn.disi.utils.tabular.TableReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class MatrixReader extends SequentialAttributeReader<double[][]> {

	private String fIndex1Key;

	private String fIndex2Key;

	private String fAttributeKey;

	private int fLastRead;

	public MatrixReader(InputStream is, String id, String index1,
			String index2, String attributeKey) throws IOException {
		this(new TableReader(is), id, index1, index2, attributeKey);
	}

	public MatrixReader(TableReader reader, String id, String index1,
			String index2, String attributeKey) throws IOException {
		super(reader, id);
		fIndex1Key = index1;
		fIndex2Key = index2;
		fAttributeKey = attributeKey;
	}

	@Override
	public double[][] read(int[] ids) throws IOException {
		double[][] w = null;

		w = new double[ids.length][ids.length];
		for (int i = 0; i < w.length; i++) {
			Arrays.fill(w[i], Double.MAX_VALUE);
		}

		fLastRead = 0;

		while (!rootChanged()) {
			int source = idOf(Integer.parseInt(get(fIndex1Key)), ids);
			int target = idOf(Integer.parseInt(get(fIndex2Key)), ids);
			double weight = Double.parseDouble(get(fAttributeKey));
			if (weight < 0) {
				throw new IllegalArgumentException(Double.toString(weight));
			}
			w[source][target] = weight;
			fLastRead++;
			advance();
		}

		return w;
	}

	public int lastRead() {
		return fLastRead;
	}

}
