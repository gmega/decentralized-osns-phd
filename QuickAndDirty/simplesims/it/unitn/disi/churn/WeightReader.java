package it.unitn.disi.churn;

import it.unitn.disi.utils.tabular.TableReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class WeightReader {

	private String fCurrentRoot;

	private TableReader fReader;

	public WeightReader(InputStream stream) throws IOException {
		fReader = new TableReader(stream);
		fReader.next();
		fCurrentRoot = fReader.get("id");
	}
	
	public boolean hasNext() {
		return fReader.hasNext();
	}
	
	public String currentRoot() {
		return fCurrentRoot;
	}

	public void skipCurrent() throws IOException {
		String root;
		while ((root = fReader.get("id")).equals(fCurrentRoot)
				&& fReader.hasNext()) {
			fReader.next();
		}
		fCurrentRoot = root;
	}

	public double[][] readWeights(int[] ids) throws IOException {
		double[][] w = null;

		w = new double[ids.length][ids.length];
		for (int i = 0; i < w.length; i++) {
			Arrays.fill(w[i], Double.MAX_VALUE);
		}

		String root = fCurrentRoot;
		for (int i = 0; (root = fReader.get("id")).equals(fCurrentRoot)
				&& fReader.hasNext(); i++) {

			int source = idOf(Integer.parseInt(fReader.get("source")), ids);
			int target = idOf(Integer.parseInt(fReader.get("target")), ids);
			double weight = Double.parseDouble(fReader.get("ttc"));
			w[source][target] = weight;
			fReader.next();
		}

		fCurrentRoot = root;
		return w;
	}

	private int idOf(int id, int[] ids) {
		for (int i = 0; i < ids.length; i++) {
			if (ids[i] == id) {
				return i;
			}
		}

		throw new NoSuchElementException();
	}

}
