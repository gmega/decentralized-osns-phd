package it.unitn.disi.graph.lightweight;

import it.unitn.disi.utils.MiscUtils;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public abstract class LSGCreator {
	
	private static final Logger fLogger = Logger.getLogger(LSGCreator.class);
	
	public LightweightStaticGraph create() {
		// Phase 1 - compute memory requirements.
		fLogger.info("1: Computing required storage.");
		CountAction ca = new CountAction();
		uncheckedGraphLoop(ca);
		
		// Phase 2 - allocates memory.
		fLogger.info("1: Maximum id is " + ca.maxId() + ".");
		
		// Computes rough memory requirements.
		long size = (ca.cells() * Integer.SIZE)/(Byte.SIZE);
		fLogger.info("1: Requires " + ca.cells() + " integers (" + size + " bytes)");
		fLogger.info("2: Allocating memory.");
		int [][] adjacency = allocate(ca.maxId() + 1, ca.sizeMap());

		// Frees up some memory. Not that it helps for us, but might help 
		// for other parts of the algorithm we don't see here.
		ca = null;

		// Phase 3 - goes again through the input, and loads the graph.
		fLogger.info("3: loading graph into memory.");
		GraphBuildAction gba = new GraphBuildAction(adjacency);
		uncheckedGraphLoop(gba);
		
		return new LightweightStaticGraph(adjacency, true);
	}
	
	private void uncheckedGraphLoop(Action action) {
		try {
			graphLoop(action);
		} catch (Exception ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}
	
	protected abstract void graphLoop(Action action) throws Exception;
	
	private int[][] allocate(int size, Map<Integer, Integer> sizes) {
		int [][] adjacency = new int[size][];
		int neighborless = 0;
		
		for (int i = 0; i < size; i++) {
			Integer neighbors = sizes.get(i);
			if (neighbors == null) {
				neighbors = 0;
			}
			
			if (neighbors == 0) {
				neighborless++;
			}
			
			adjacency[i] = new int[neighbors];
		}
		
		if (neighborless > 0) {
			fLogger.warn("Warning: there were (" + neighborless
					+ ") nodes without neighbors (ID holes?).");
		}
		
		return adjacency;
	}

	interface Action {
		public void innerAction(int source, int target);
	}
	
	private class CountAction implements Action {
		
		private final HashMap<Integer, Integer> fSizes = new HashMap<Integer,Integer>();
		
		private int fCells;
		
		private int fMaxId = Integer.MIN_VALUE;

		@Override
		public void innerAction(int source, int target) {
			Integer sourceCount = fSizes.get(source);
			if (sourceCount == null) {
				sourceCount = 0;
			}
			fCells++;
			fSizes.put(source, sourceCount + 1);
			fMaxId = Math.max(source, fMaxId);
			fMaxId = Math.max(target, fMaxId);
		}
		
		public int cells() {
			return fCells;
		}
		
		public int maxId() {
			return fMaxId;
		}
		
		public Map<Integer, Integer> sizeMap() {
			return fSizes;
		}
	}
	
	private class GraphBuildAction implements Action {
		
		private int [][] fAdjacency;
		
		private int [] fCounters;
		
		public GraphBuildAction(int [][] adjacency) {
			fCounters = new int[adjacency.length];
			fAdjacency = adjacency;
		}

		@Override
		public void innerAction(int source, int target) {
			fAdjacency[source][fCounters[source]++] = target;
		}
	}
}
