package it.unitn.disi.churn.config;

import it.unitn.disi.utils.tabular.TableReader;

import java.io.IOException;
import java.io.InputStream;

public class AssignmentReader extends SequentialAttributeReader<Object> {

	public AssignmentReader(InputStream is, String id) throws IOException {
		super(is, id);
	}

	public AssignmentReader(TableReader reader, String id) throws IOException {
		super(reader, id);
	}

	@Override
	public Assignment read(int[] ids) throws IOException {

		Assignment assignment = new Assignment(ids.length);
		for (int i = 0; !rootChanged(); i++) {
			int node = Integer.parseInt(get("node"));
			node = ids != null ? idOf(node, ids) : node;
			assignment.li[node] = Double.parseDouble(get("li"));
			assignment.di[node] = Double.parseDouble(get("di"));
			assignment.nodes[i] = node;
			advance();
		}

		return assignment;
	}

	public static class Assignment {
		public final double[] li;
		public final double[] di;
		public final int[] nodes;

		public Assignment(int size) {
			this.li = new double[size];
			this.di = new double[size];
			this.nodes = new int[size];
		}
	}
}
