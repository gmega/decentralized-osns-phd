package it.unitn.disi;

import it.unitn.disi.graph.SubgraphDecorator;
import it.unitn.disi.graph.codecs.AdjListGraphDecoder;
import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.utils.ResettableFileInputStream;
import it.unitn.disi.utils.SimpleScheduler;
import it.unitn.disi.utils.collections.Pair;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import peersim.graph.Graph;

/**
 * {@link DiffusionSimulator} implements an iterative, brute-force algorithm to
 * the problem of answering the minimum number of rounds required to diffuse a
 * piece of information over an arbitrary graph.
 * 
 * @author giuliano
 */
public class DiffusionSimulator {
	
	public static void main(String [] args) throws IOException {
		
		File graph = null;
		File schedule = null;
		Integer cores = null;
		
		switch(args.length) {
		case 3:
			cores = Integer.parseInt(args[2]);
		case 2:
			graph = new File(args[0]);
			schedule = new File(args[1]);
			break;
		default:
			System.err.println("Invalid arguments.");
			System.exit(-1);
		}
		
		Graph g = LightweightStaticGraph.load(new AdjListGraphDecoder(new ResettableFileInputStream(graph)));
		if (cores == null) {
			cores = Runtime.getRuntime().availableProcessors();
		}
		
		DiffusionSimulator dsd = new DiffusionSimulator(g, SimpleScheduler
				.fromFile(schedule));
		dsd.run(cores);
	}
	
	private int fCount = 1;
	
	private final Graph fGraph;
	
	private final Iterator<Integer> fSchedule;
	
	private SubgraphDecorator [] fDecorators;
	                                 
	public DiffusionSimulator(Graph g, Iterator<Integer> schedule) {
		fSchedule = schedule;
		fGraph = g;
	}
	
	private void run(int cores) {
		
		Thread [] workers = new Thread[cores];
		fDecorators = new SubgraphDecorator[cores];
		
		for (int i = 0; i < workers.length; i++) {
			synchronized(this) {
				// Safe publishing of the decorators to the workers.
				fDecorators[i] = new SubgraphDecorator(fGraph, false);
			}
			workers[i] = new Thread(new DiffusionWorker(this, i));
			workers[i].start();
		}
		
		for (int i = 0; i < workers.length; i++) {
			try {
				workers[i].join();
			} catch(InterruptedException ex) {
				// Don't really care about this. 
			}
		}
	}
	
	synchronized Pair<Integer, int[][]> work(DiffusionWorker worker) {
		
		if (!fSchedule.hasNext()) {
			return null;
		}
		
		SubgraphDecorator decorator = fDecorators[worker.id];	
		int next = fSchedule.next();
		Set<Integer> vertices = new HashSet<Integer>();
		for (Integer neighbor : fGraph.getNeighbours(next)) {
			vertices.add(neighbor);
		}
		vertices.add(next);
		decorator.setVertexList(vertices);
		
		// Creates the fast-access subgraph.
		int [][] graph = new int[decorator.size()][];
		for (int i = 0; i < graph.length; i++) {
			
			Collection<Integer> neighbors = decorator.getNeighbours(i);
			graph[i] = new int[decorator.getNeighbours(i).size()];
			Iterator<Integer> it = neighbors.iterator();
			
			for (int j = 0; it.hasNext(); j++) {
				graph[i][j] = it.next();
			}
			
			// Just a little sanity check.
			if (it.hasNext()) {
				throw new IllegalStateException();
			}
		}
		
		System.err.println("Scheduled unit " + (fCount++) + " to worker "
				+ worker.id + " (size " + graph.length + ").");
		
		return new Pair<Integer, int[][]> (decorator.idOf(next), graph);
	}
	
	synchronized void newTMax(int tMax, int[] paintedBy, int[] paintSchedule, DiffusionWorker reporter) {
		print("TMAX", Integer.toString(tMax), paintedBy, paintSchedule, reporter);
	}
	
	synchronized void newTAvg(double tAvg, int[] paintedBy, int[] paintSchedule, DiffusionWorker reporter) {
		print("TAVG", Double.toString(tAvg), paintedBy, paintSchedule, reporter);
	}
	
	private void print(String tag, String quantity, int[] paintedBy, int[] paintSchedule, DiffusionWorker reporter) {
		SubgraphDecorator decorator = fDecorators[reporter.id];
		Integer root = findRoot(paintSchedule);
		root = decorator.inverseIdOf(root);
		
		StringBuffer buffer = new StringBuffer();
		buffer.append(tag);
		buffer.append(" ");
		buffer.append(root);
		buffer.append(" ");
		buffer.append(quantity);
		buffer.append("\nSCHED ");
		buffer.append(root);
		buffer.append(" ");
		
		for (int i = 0; i < paintSchedule.length; i++) {
			String painter = paintedBy[i] == -1 ? "root" : Integer
					.toString(decorator.inverseIdOf(paintedBy[i]));
			buffer.append(painter);
			buffer.append(":");
			buffer.append(decorator.inverseIdOf(i));
			buffer.append(":");
			buffer.append(paintSchedule[i]);
			buffer.append(",");
		}
		
		buffer.deleteCharAt(buffer.length() - 1);
		System.out.println(buffer.toString());
	}

	private Integer findRoot(int[] paintSchedule) {
		Integer root = null;
		// Finds the root.
		for (int i = 0; i < paintSchedule.length; i++) {
			if(paintSchedule[i] == -1) {
				root = i;
			}
		}
		return root;
	}
}

class DiffusionWorker implements Runnable {
	
	private static final int UNPAINTED = Integer.MAX_VALUE;

	public final int id;
	
	private final DiffusionSimulator fParent;
	
	private int [][] fGraph;
	
	private int [] fPainted;
	
	private int [] fPaintedBy;
	
	private int [][] fCounters;
	
	private int fPaintCount = 0;
	
	private int fMax;
	
	private double fAvg;
	
	DiffusionWorker (DiffusionSimulator parent, int id) {
		fParent = parent;
		this.id = id;
	}
	
	@Override
	public void run() {
		System.err.println("Started worker (" + id + ").");
		Pair<Integer, int[][]> work;
		while((work = fParent.work(this)) != null) {
			init(work.b);
			paint(work.a);
			runSim();
		}
		System.err.println("Worker (" + id + ") is done.");
	}
	
	private void init(int [][] graph) {
		int size = graph.length;
		// Allocates paint bits and counters.
		fCounters = new int[size][];
		fPainted = new int[size];
		fPaintedBy = new int[size];
		for (int i = 0; i < size; i++) {
			fPainted[i] = UNPAINTED;
			fCounters[i] = new int[size];
		}
		
		fMax = Integer.MAX_VALUE;
		fAvg = Double.MAX_VALUE;
		fPaintCount = 0;
		fGraph = graph;
	}
	
	private void paint(int idx) {
		fPainted[idx] = -1;
		fPaintedBy[idx] = -1;
		fPaintCount++;
	}
	
	public void runSim() {

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
							fPaintedBy[neighbor] = i;
							fPaintCount++;
							progress = true;
							break;
						}
					}
				}
			}

			// No progress in this step.
			if (!progress) {
				// This is an end state. 
				if (fPaintCount == fPainted.length) {
					// The graph is fully painted.
					// Computes the average latency.
					double avg = 0.0;
					for (int i = 0; i < fPainted.length; i++) {
						avg += fPainted[i];
					}
					avg++; // Re-sums the -1.0 from the root.
					
					// New best t_max?
					if (step < fMax) {
						fMax = step;
						fParent.newTMax(fMax, fPaintedBy, fPainted, this);
					}
					
					// New best t_avg?
					if (avg < fAvg) {
						fAvg = avg;
						fParent.newTAvg(fAvg/(fPainted.length - 1), fPaintedBy, fPainted, this);
					}
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
