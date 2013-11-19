package it.unitn.disi.churn.diffusion.graph;

import java.io.RandomAccessFile;

import it.unitn.disi.graph.generators.InMemoryProvider;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.streams.EOFException;
import it.unitn.disi.utils.tabular.TableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;

@AutoConfig
public class DynamicDegreeComputer implements Runnable {

	private InMemoryProvider fProvider;

	private double[] fAssignments;

	public DynamicDegreeComputer(@Attribute("graph") String graph,
			@Attribute("assignments") String assignments) throws Exception {

		fProvider = new InMemoryProvider(graph);
		fAssignments = loadAssignments(assignments);
	}

	@Override
	public void run() {
		try {
			run0();
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	public void run0() throws Exception {

		TableWriter writer = new TableWriter(System.out, "id", "degree",
				"dynamicdegree");

		for (int i = 0; i < fProvider.size(); i++) {
			int[] vertices = fProvider.verticesOf(i);

			if (vertices[0] != i) {
				throw new IllegalStateException("Unsupported provider.");
			}

			writer.set("id", i);
			writer.set("degree", vertices.length - 1);
			writer.set("dynamicdegree", dynamicDegree(vertices));
			writer.emmitRow();
		}
	}

	private double[] loadAssignments(String fileName) throws Exception {
		double[] assignments = new double[fProvider.size() * 2];

		System.err.print("-- Load assignments...");
		RandomAccessFile file = null;
		try {
			file = new RandomAccessFile(fileName, "r");
			for (int i = 0; i < assignments.length; i++) {
				assignments[i] = file.readDouble();
			}
			System.err.println(" done.");
			return assignments;
		} catch (EOFException ex) {
			System.err.println("Stream terminated unexpectedly. Aborting.");
			throw ex;
		} finally {
			MiscUtils.safeClose(file, true);
		}
	}

	private int dynamicDegree(int[] vertices) {
		double degree = 0.0;
		for (int i = 1; i < vertices.length; i++) {
			int idx = vertices[i];
			double li = fAssignments[2 * idx];
			double di = fAssignments[2 * idx + 1];

			degree += (li / (li + di));
		}

		return MiscUtils.safeCast(Math.max(1, Math.round(degree)));
	}

}
