package it.unitn.disi.churn.config;

import it.unitn.disi.utils.tabular.TableReader;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Experiment implements Serializable {

	private static final long serialVersionUID = 1L;

	public final int root;

	public final Map<String, String> attributes;

	public final double[] lis;
	public final double[] dis;

	public Experiment(int root, TableReader source, double[] lis, double[] dis) {
		this.root = root;

		HashMap<String, String> attributes = new HashMap<String, String>();
		for (String key : source.columns()) {
			attributes.put(key, source.get(key));
		}
		this.attributes = Collections.unmodifiableMap(attributes);

		this.lis = lis;
		this.dis = dis;
	}

	public String toString() {
		StringBuffer info = new StringBuffer();
		info.append("root: ");
		info.append(root);
		if (lis != null) {
			info.append(", size: ");
			info.append(lis.length);
		}
		return info.toString();
	}
}
