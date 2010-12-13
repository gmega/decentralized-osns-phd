package it.unitn.disi.graph.cli;

import it.unitn.disi.graph.lightweight.LightweightStaticGraph;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Stack;

import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class ComputeConnectivity extends GraphAnalyzer {

	private LightweightStaticGraph fGraph;
	
	public ComputeConnectivity(@Attribute("decoder") String decoder) {
		super(decoder);
	}

	int compute(OutputStream oStream) throws IOException {
		PrintStream out = new PrintStream(oStream);

		int components = 0;
		System.err.println("Running Tarjan's algorithm.");
		try {
			out.println("id components degree");
			for (int i = 0; i < fGraph.size(); i++) {
				out.println(i + " " + tarjan(fGraph.fastGetNeighbours(i)) + " "
						+ fGraph.fastGetNeighbours(i).length + "\n");
			}
		} finally {
			out.close();
		}
		return components;
	}

	private int fVisitCounter = 0;

	private int[] fVisit;
	private int[] fLowLink;

	private boolean[] fSubgraph;
	private boolean[] fOnStack;

	private Stack<Integer> fStack = new Stack<Integer>();
	private Stack<Integer> fDfsCounterStack = new Stack<Integer>();
	private Stack<Integer> fDfsElementStack = new Stack<Integer>();

	void initTarjan() {
		fVisit = new int[fGraph.size()];
		fLowLink = new int[fGraph.size()];
		fOnStack = new boolean[fGraph.size()];
		fSubgraph = new boolean[fGraph.size()];

		for (int i = 0; i < fGraph.size(); i++) {
			fVisit[i] = -1;
			fLowLink[i] = i;
		}
	}

	int tarjan(int[] subgraph) {

		fStack.clear();
		fDfsCounterStack.clear();
		fDfsElementStack.clear();
		fVisitCounter = 0;

		for (int element : subgraph) {
			fSubgraph[element] = true;
		}

		int ncomps = 0;
		for (int element : subgraph) {
			if (fVisit[element] == -1) {
				ncomps += tarjan(element, fSubgraph);
			}
		}

		for (int element : subgraph) {
			fSubgraph[element] = false;
			fVisit[element] = -1;
			fLowLink[element] = element;
		}

		return ncomps;
	}

	int tarjan(int starting, boolean[] subgraph) {

		int upstream = 0;
		fDfsCounterStack.push(0);
		fDfsElementStack.push(starting);

		dfs: while (true) {

			int node = fDfsElementStack.peek();

			if (fVisit[node] == -1) {
				fLowLink[node] = fVisit[node] = fVisitCounter++;
				fStack.push(node);
				fOnStack[node] = true;
			}

			for (int c = fDfsCounterStack.peek(); c < fGraph.degree(node); c++) {
				int neighbor = fGraph.fastGetNeighbours(node)[c];
				if (!subgraph[neighbor]) {
					continue;
				}
				if (fVisit[neighbor] == -1) {
					fDfsCounterStack.set(fDfsCounterStack.size() - 1, c + 1);
					fDfsElementStack.push(neighbor);
					fDfsCounterStack.push(0);
					continue dfs;
				} else if (fOnStack[neighbor]
						&& (fVisit[neighbor] < fVisit[node])) {
					fLowLink[node] = Math.min(fLowLink[node], fVisit[neighbor]);
				}
			}

			// SCC root
			if (fVisit[node] == fLowLink[node]) {
				upstream++;
				int popped;
				do {
					popped = fStack.pop();
					fOnStack[popped] = false;
				} while (popped != node);
			}

			int neighbor = fDfsElementStack.pop();
			if (fDfsElementStack.isEmpty()) {
				break;
			}
			fDfsCounterStack.pop();
			node = fDfsElementStack.peek();
			fLowLink[node] = Math.min(fLowLink[node], fLowLink[neighbor]);
		}

		return upstream;
	}
	
	void setBaseGraph(LightweightStaticGraph graph) {
		fGraph = graph;
	}

	@Override
	protected void transform(LightweightStaticGraph graph, OutputStream oup)
			throws IOException {
		setBaseGraph(graph);
		initTarjan();
		compute(oup);
	}
}
