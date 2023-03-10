package it.unitn.disi.graph.lightweight;

import it.unitn.disi.graph.IGraphVisitor;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;

import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public abstract class LSGCreator {

	private static final Logger fLogger = Logger.getLogger(LSGCreator.class);

	public LightweightStaticGraph create(boolean quiet) {
		Level current = fLogger.getLevel();
		if (quiet) {
			fLogger.setLevel(Level.ERROR);
		}
		try {
			return create();
		} finally {
			fLogger.setLevel(current);
		}
	}

	public LightweightStaticGraph create() {

		// Phase 1 - compute memory requirements.
		fLogger.info("1: Computing required storage.");
		CountAction ca = new CountAction();

		uncheckedGraphLoop(ca);

		// Phase 2 - allocates memory.
		fLogger.info("1: Maximum id is " + ca.maxId() + ".");

		// Computes rough memory requirements.
		long size = MiscUtils.integers(ca.cells())
				+ MiscUtils.pointers(ca.maxId() + 1);
		fLogger.info("1: Graph has " + ca.maxId() + " ids and " + ca.cells()
				+ " edges. Memory required: " + size + " bytes.");
		fLogger.info("2: Allocating memory.");

		int[] sizes = ca.sizeMap();
		// Frees up some memory.
		ca = null;

		int[][] adjacency = allocate(sizes);

		// Phase 3 - goes again through the input, and loads the graph.
		fLogger.info("3: loading graph into memory.");
		GraphBuildAction gba = new GraphBuildAction(adjacency);
		uncheckedGraphLoop(gba);

		return new LightweightStaticGraph(adjacency);
	}

	private void uncheckedGraphLoop(IGraphVisitor action) {
		try {
			graphLoop(action);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	protected abstract void graphLoop(IGraphVisitor action) throws Exception;

	private int[][] allocate(int[] sizes) {
		int[][] adjacency = new int[sizes.length][];
		int neighborless = 0;

		fLogger.info("2: Allocated pointer array.");

		IProgressTracker tracker = Progress.newTracker("allocating cells",
				sizes.length, fLogger);
		tracker.startTask();
		for (int i = 0; i < sizes.length; i++) {
			Integer neighbors = sizes[i];
			if (neighbors == 0) {
				neighborless++;
			}
			adjacency[i] = new int[neighbors];
			tracker.tick();
		}
		tracker.done();

		if (neighborless > 0) {
			fLogger.warn("Warning: there were (" + neighborless
					+ ") nodes without neighbors (ID holes?).");
		}

		return adjacency;
	}

	private class CountAction implements IGraphVisitor {

		private ArrayList<Integer> fSizes = new ArrayList<Integer>();

		private int fCells;

		private int fMaxId = Integer.MIN_VALUE;

		@Override
		public void visitEdge(int source, int target) {
			visitVertex(source);
			visitVertex(target);
			int sourceCount = fSizes.get(source);
			fCells++;
			fSizes.set(source, sourceCount + 1);
		}

		@Override
		public void visitVertex(int id) {
			fMaxId = Math.max(id, fMaxId);
			if (fSizes.size() <= fMaxId) {
				MiscUtils.grow(fSizes, fMaxId + 1, 0);
			}
		}

		public int cells() {
			return fCells;
		}

		public int maxId() {
			return fMaxId;
		}

		public int[] sizeMap() {
			int[] sizes = new int[fMaxId + 1];
			for (int i = 0; i < sizes.length; i++) {
				sizes[i] = fSizes.get(i);
			}
			return sizes;
		}
	}

	private class GraphBuildAction implements IGraphVisitor {

		private int[][] fAdjacency;

		private int[] fCounters;

		public GraphBuildAction(int[][] adjacency) {
			fCounters = new int[adjacency.length];
			fAdjacency = adjacency;
		}

		@Override
		public void visitEdge(int source, int target) {
			fAdjacency[source][fCounters[source]++] = target;
		}

		@Override
		public void visitVertex(int id) {
		}
	}
}
