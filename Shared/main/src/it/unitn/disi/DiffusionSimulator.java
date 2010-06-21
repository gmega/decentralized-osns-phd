package it.unitn.disi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/**
 * {@link DiffusionSimulator} implements an iterative, brute-force algorithm to
 * the problem of answering the minimum number of rounds required to diffuse a
 * piece of information over an arbitrary graph.
 * 
 * @author giuliano
 */
public class DiffusionSimulator {
	
	private static final int UNPAINTED = Integer.MAX_VALUE;

	private int[][] fGraph;
	
	private int[] fPainted;
	private int[][] fCounters;
	private int fPaintCount = 0;

	private int fMax;

	/**
	 * The command takes one single parameter: a text file, encoding a graph as
	 * an adjacency list represented by sequence of integers.
	 * 
	 * @throws IOException
	 *             if the provided file cannot be read.
	 */
	public static void main(String[] args) throws IOException {
		DiffusionSimulator ds = new DiffusionSimulator();
		
		if (args.length < 1) {
			System.err.println("Missing graph file.");
		}
		
		if (ds.load(new File(args[0]))) {
			ds.runAll();
		}

		System.out.println(ds.fMax);
	}

	public void runAll() {
		fMax = Integer.MAX_VALUE;
		
		for (int i = 0; i < fGraph.length; i++) {
			init();
			paint(i);
			run();
		}
	}

	public void paint(int idx) {
		fPainted[idx] = -1;
		fPaintCount++;
	}

	public void init() {
		for (int i = 0; i < fGraph.length; i++) {
			fPainted[i] = UNPAINTED;
			for (int j = 0; j < fCounters[i].length; j++) {
				fCounters[i][j] = 0;
			}
		}
		
		fPaintCount = 0;
	}
	
	public boolean load(File file) throws IOException {

		LineNumberReader reader = new LineNumberReader(new FileReader(file));

		String header = reader.readLine();
		if (header == null) {
			return false;
		}

		int size = Integer.parseInt(header);
		fGraph = new int[size][];
		fCounters = new int[size][];

		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] data = line.split(" ");
			int source = Integer.parseInt(data[0]);
			fGraph[source] = new int[data.length - 1];

			for (int i = 1; i < data.length; i++) {
				fGraph[source][i - 1] = Integer.parseInt(data[i]);
			}
		}
		
		// Allocates paint bits and counters.
		fPainted = new int[fGraph.length];
		for (int i = 0; i < size; i++) {
			fPainted[i] = UNPAINTED;
			fCounters[i] = new int[size];
		}

		return true;
	}

	public void run() {

		int step = 0;

		while (true) {
			boolean progress = false;
			for (int i = 0; i < fPainted.length; i++) {
				// Node was painted in the last step or before, so it should do a round.
				if (fPainted[i] <= step) {
					// Find unpainted neighbor.
					while(fCounters[step][i] < fGraph[i].length) {
						int neighbor = fGraph[i][fCounters[step][i]++];
						if (fPainted[neighbor] == UNPAINTED) {
							// Paints neighbor.
							fPainted[neighbor] = (step + 1);
							fPaintCount++;
							progress = true;
							break;
						}
					}
				}
			}

			// No progress in this step.
			if (!progress) {
				// This is an end state. Register the number of steps,
				// if it's the smallest we've ever seen.
				if (fPaintCount == fPainted.length && step < fMax) {
					fMax = step;
					System.out.println("MAX:" + step);
				}
				
				// Undoes this step.
				for (int i = 0; i < fPainted.length; i++) {
					// Unpaints what's been painted in this step.
					if (fPainted[i] == step) {
						fPainted[i] = UNPAINTED;
						fPaintCount--;
					}
					
					// Clears all counters.
					if (fCounters[step][i] != 0) {
						fCounters[step][i] = 0;
					}
				}
				
				// One step back.
				step--;
				
				// OK, base achieved. Return.
				if (step == -1) {
					break;
				}
			
			} else {
				step++;
			}
		}
	}
}
