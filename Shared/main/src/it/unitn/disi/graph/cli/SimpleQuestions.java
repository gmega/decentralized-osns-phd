package it.unitn.disi.graph.cli;

import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.graph.Graph;

/**
 * Answers three simple questions about a graph: whether it is simple, and
 * whether it is directed, and how many edges/vertices it has.
 * 
 * @author giuliano
 */
@AutoConfig
public class SimpleQuestions extends GraphAnalyzer {

	public SimpleQuestions(@Attribute("decoder") String decoder) {
		super(decoder);
	}
	
	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream oup)
			throws IOException {
		PrintStream p = new PrintStream(oup);
		p.println(graph.isSimple() ? "Simple." : "Non-simple.");
		p.println(graph.directed() ? "Directed." : "Undirected.");
		componentList(graph, p);
		System.out.println("Graph has [" + graph.size() + "] vertices and ["
				+ graph.edgeCount() + "] edges (directed).");
	}
	
	private void componentList(Graph graph, PrintStream oup) {
		int [] components = GraphAlgorithms.components(graph);
		for (int i = 0; i < components.length; i++) {
			if (components[i] != 0) {
				oup.print("[" + components[i] + "] ");
			}
		}
		oup.println("");
	}
}
