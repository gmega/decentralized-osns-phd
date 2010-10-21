package it.unitn.disi.graph;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;

import peersim.graph.Graph;

public class GraphWriter {
	
	public static void printAdjList(Graph g, Writer writer) {
		PrintWriter ps = new PrintWriter(writer);
		for (int i = 0; i < g.size(); i++) {
			StringBuffer buf = new StringBuffer();
			buf.append(i);
			buf.append(" ");
			for (int neighbor : g.getNeighbours(i)) {
				buf.append(neighbor);
				buf.append(" ");
			}
			buf.deleteCharAt(buf.length() - 1);
			ps.println(buf.toString());
		}
		ps.flush();
	}
	
	
	public static void printAdjList(SubgraphDecorator mapper, Graph g, OutputStreamWriter writer)
			throws IOException {
		PrintWriter ps = new PrintWriter(writer);
		for (int i = 0; i < g.size(); i++) {
			StringBuffer buf = new StringBuffer();
			buf.append(mapper.inverseIdOf(i));
			buf.append(" ");
			for (int neighbor : g.getNeighbours(i)) {
				buf.append(mapper.inverseIdOf(neighbor));
				buf.append(" ");
			}
			buf.deleteCharAt(buf.length() - 1);
			ps.println(buf.toString());
		}
		ps.flush();
	}
}
