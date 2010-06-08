package it.unitn.disi.utils;

import it.unitn.disi.codecs.GraphDecoder;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;

import peersim.core.Linkable;
import peersim.core.Node;
import peersim.graph.NeighbourListGraph;

public class MiscUtils {
	
	public static void safeClose(Closeable is, boolean rethrow) {
		try {
			if (is != null) {
				is.close();
			}
		} catch (IOException ex) {
			if (rethrow) {
				throw new RuntimeException(ex);
			}
			ex.printStackTrace();
		}
	}
	
	public static NeighbourListGraph load(NeighbourListGraph graph, GraphDecoder dec) {
		while (dec.hasNext()) {
			int source = dec.getSource();
			int target = dec.next();
			grow(graph, Math.max(source, target));
			graph.setEdge(source, target);
		}
		
		return graph;
	}

	private static void grow(NeighbourListGraph graph, int max) {
		while (graph.size() <= max) {
			graph.addNode(new Object());
		}
	}
	
	public static int compact(Object[] array, IExchanger exch, int end) {
		int gap = 0;
		
		for (int i = 0; i < end; i++) {
			if (array[i] == null) {
				gap++;
				continue;
			}
			if (gap == 0) {
				continue;
			}
			
			exch.exchange(i - gap, i);
		}
		
		return end - gap;
	}
	
	
	public static int compact(Object[] array, int end) {
		int gap = 0;
		
		for (int i = 0; i < end; i++) {
			if (array[i] == null) {
				gap++;
				continue;
			}
			if (gap == 0) {
				continue;
			}
			
			array[i - gap] = array[i];
			array[i] = null;
		}
		
		return end - gap;
	}
	
	public static int compact(ArrayList<Node> array, int end) {
		int gap = 0;
		
		for (int i = 0; i < end; i++) {
			if (array.get(i) == null) {
				gap++;
				continue;
			}
			if (gap == 0) {
				continue;
			}
		}
		
		return end - gap;
	}

	public static int countIntersections(Linkable l1, Node n2, int linkableId) {
		return countIntersections(l1, (Linkable) n2.getProtocol(linkableId), null, false);
	}
	
	public static int countIntersections(Node n1, Node n2, int linkableId) {
		return countIntersections((Linkable) n1.getProtocol(linkableId),
				(Linkable) n2.getProtocol(linkableId), null, false);
	}
	
	public static int countIntersections(Linkable l1, Linkable l2, ArrayList<Object> store, boolean id) {
		int ourSize = l1.degree();
		int inCommon = 0;

		for (int i = 0; i < ourSize; i++) {
			Node neighbor = l1.getNeighbor(i);
			if (l2.contains(neighbor)) {
				if (store != null) {
					if (id) {
						store.add(neighbor.getID());
					} else {
						store.add(neighbor);
					}
				}
				inCommon++;
			}
		}

		return inCommon;
	}
	
	public static double log2(double input) {
		return Math.log(input)/Math.log(2.0);
	}
}
