package it.unitn.disi.sps.newscast;

import it.unitn.disi.graph.SubgraphDecorator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.reports.GraphObserver;
import peersim.util.IncrementalStats;

/**
 * 
 * @author giuliano
 */
public class ConnectednessObserver extends GraphObserver {

	private static final int FLUSH_INTERVAL = 50000;
	
	private static final String PAR_MODE = "mode";

	private static final String PAR_OUTPUT_FOLDER = "folder";

	private static final String PAR_ID = "id";

	private static final int CONNECTEDNESS_DATAPOINTS = 0x1;
	
	private static final int CONNECTEDNESS_INVERT = 0x2;

	private SubgraphDecorator fSubgraph;

	private int fMode;

	private int fId;

	private String fFolder;

	public ConnectednessObserver(String prefix) {
		super(prefix);
		fMode = Configuration.getInt(prefix + "." + PAR_MODE);
		if (outputs()) {
			fFolder = Configuration.getString(prefix + "." + PAR_OUTPUT_FOLDER);
			if (Configuration.contains(prefix + "." + PAR_ID)) {
				fId = Configuration.getInt(prefix + "." + PAR_ID);
			} else {
				// Picks a random positive integer.
				fId = Math.abs(CommonState.r.nextInt());
			}
		}
	}

	public boolean execute() {
		updateGraph();

		IncrementalStats stats = new IncrementalStats();
		StringBuffer connPoints = new StringBuffer();

		for (int i = 0; i < g.size(); i++) {
			SubgraphDecorator subg = getSubgraph();
			Collection<Integer> c = g.getNeighbours(i);
			subg.setVertexList(c);
			
			double connectedness = ga.tarjan(getSubgraph()).size();
			
			if(invert()) {
				connectedness /= 1.0;
			}

			if (outputs()) {
				connPoints.append(i);
				connPoints.append(" ");
				connPoints.append(connectedness);
				connPoints.append(" ");
				connPoints.append(c.size());
				connPoints.append("\n");
			}

			stats.add(connectedness);
			
			if ((i % FLUSH_INTERVAL) == 0 && outputs()) {
				printToFile(connPoints);
				connPoints = new StringBuffer();
			}
		}

		if (outputs()) {
			printToFile(connPoints);
		}

		System.out.println(name + ": " + stats);
		return false;
	}

	private boolean invert() {
		return (fMode & CONNECTEDNESS_INVERT) != 0;
	}

	private boolean outputs() {
		return (fMode & CONNECTEDNESS_DATAPOINTS) != 0;
	}

	private SubgraphDecorator getSubgraph() {
		if (fSubgraph == null || fSubgraph.getGraph() != g) {
			fSubgraph = new SubgraphDecorator(g, true);
		}

		return fSubgraph;
	}

	private void printToFile(StringBuffer connPoints) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(new File(fFolder, "connectedness_" + fId
					+ "_" + peersim.core.CommonState.getTime()), true);
			writer.write(connPoints.toString());
		} catch (IOException ex) {
			System.out.println("Failed to write output file at cycle "
					+ peersim.core.CommonState.getTime()
					+ ". Reports will be disabled.");
			fMode = 0;
			ex.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
}
