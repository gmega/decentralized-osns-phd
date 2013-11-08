package it.unitn.disi.churn.diffusion;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.algorithms.GraphAlgorithms;
import it.unitn.disi.graph.algorithms.GraphAlgorithms.IEdgeFilter;
import it.unitn.disi.simulator.core.INetwork;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;

/**
 * Port of the P2P'11 ComponentSelector. This is a simple implementation, which
 * will not work if:
 * <ol>
 * <li>there is churn;</li>
 * <li>the source is not the node at the center of the egonet.</li>
 * </ol>
 * 
 * @author giuliano
 */
public class ComponentSelector implements IPeerSelector{

	private static final BitSet ALL_DONE = null;

	private IPeerSelector fDelegate;

	private IndexedNeighborGraph fCached;

	private ArrayList<BitSet> fMembership;

	private BitSet fForbidden = new BitSet();
	
	public ComponentSelector(IPeerSelector delegate) {
		fDelegate = delegate;
	}

	@Override
	public int selectPeer(int selecting, IndexedNeighborGraph graph,
			BitSet forbidden, INetwork sim) {

		if (isNew(graph)) {
			fMembership = recomputeComponents(selecting, graph);
			fForbidden.clear();
			fCached = graph;
		}

		BitSet component = nextComponent();
		if (component == ALL_DONE) {
			return fDelegate.selectPeer(selecting, graph, forbidden, sim);
		}
		
		// Forbids everybody except those in the current component.
		fForbidden.set(0, graph.size(), true);
		fForbidden.xor(component);
		
		// Nodes forbidden upfront by the client remain forbidden.
		fForbidden.or(forbidden);

		// Picks a node in using anticentrality.
		return fDelegate.selectPeer(selecting, graph, fForbidden, sim);

	}

	private ArrayList<BitSet> recomputeComponents(final int root,
			IndexedNeighborGraph graph) {
		
		if (graph.directed()) {
			throw new IllegalArgumentException(
					"ComponentSelector does not work "
							+ "on directed graphs.");
		}

		// Edges in and out of the root are forbidden.
		IEdgeFilter sourceExclude = new IEdgeFilter() {
			@Override
			public boolean isForbidden(int i, int j) {
				return i == root || j == root;
			}
		};

		// Nodes already known to be part of a component.
		BitSet explored = new BitSet();

		ArrayList<BitSet> membership = new ArrayList<BitSet>();
		for (int i = 0; i < graph.size(); i++) {
			// The root is out.
			if (i == root) {
				continue;
			}

			// Node already part of a component, skip it.
			if (explored.get(i)) {
				continue;
			}

			// New component.
			BitSet reachable = new BitSet();
			GraphAlgorithms.dfs(graph, i, reachable, sourceExclude);
			membership.add(reachable);
			explored.or(reachable);
		}

		// Sorts by size.
		Collections.sort(membership, new Comparator<BitSet>() {
			@Override
			public int compare(BitSet o1, BitSet o2) {
				return o1.cardinality() - o2.cardinality();
			}
		});
		
		return membership;
	}
	
	private BitSet nextComponent() {
		if (fMembership.size() > 0) {
			return fMembership.remove(fMembership.size() - 1);
		}
		
		return ALL_DONE;
	}

	private boolean isNew(IndexedNeighborGraph graph) {
		return graph != fCached;
	}

}
