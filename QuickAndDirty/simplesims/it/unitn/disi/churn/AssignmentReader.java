package it.unitn.disi.churn;

import java.io.IOException;
import java.io.InputStream;

public class AssignmentReader extends SequentialAttributeReader<Object> {

	public AssignmentReader(InputStream stream, String id) throws IOException {
		super(stream, id);
	}

	@Override
	public Assignment read(int[] ids) throws IOException {

		Assignment assignment = new Assignment(ids.length);
		for (int i = 0; !rootChanged(); i++) {
			int node = idOf(Integer.parseInt(get("node")), ids);
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
