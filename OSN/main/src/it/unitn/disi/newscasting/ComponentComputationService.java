package it.unitn.disi.newscasting;

import it.unitn.disi.graph.GraphProtocol;
import it.unitn.disi.utils.graph.SubgraphDecorator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.core.Protocol;
import peersim.graph.Graph;
import peersim.graph.GraphAlgorithms;

public class ComponentComputationService implements Protocol {

	private static final String PAR_SN = "fast_graph_protocol";
	
	private static GraphAlgorithms fGa = new GraphAlgorithms();
	
	private HashMap<Integer, Integer> fComponentTable = new HashMap<Integer, Integer>();
	
	private ArrayList<Integer> [] fMappings;
	
	private int fLinkableId;
	
	private int fLastCheck = -1;
	
	public ComponentComputationService(String prefix) {
		fLinkableId = Configuration.getPid(prefix + "." + PAR_SN);
	}

	/**
	 * @param node
	 *            the {@link Node} instance that "owns" this {@link Protocol}
	 *            instance.
	 * 
	 * @return the number of connected components in the owner's neighborhood.
	 * <BR><BR>
	 * NOTE: if this method is called with a node OTHER THAN the owner node, 
	 * the behavior of the service becomes unspecified. 
	 */
	public int components(Node node) {
		check(node);
		return fMappings.length; 
	}

	/**
	 * Given a central {@link Node} and an ID number for a {@link Node} in the
	 * neighborhood of the central node (as returned by {@link Node#getID()}, and
	 * cast to {@link Integer}), returns the index of the connected component that
	 * the neighboring node belongs to.
	 */
	public int componentOf(Node central, int id) {
		check(central);
		return fComponentTable.get(id);
	}

	/**
	 * Given a central node and a component index, returns the list of node IDs
	 * (cast to {@link Integer}) which make up that component.
	 * 
	 * @param central
	 * @param id
	 * @return
	 */
	public List<Integer> members(Node central, int id) {
		check(central);
		return fMappings[id];
	}
	
	private void check(Node node) {
		if (fLastCheck == CommonState.getTime()) {
			return;
		}
		
		GraphProtocol lnk = (GraphProtocol) node.getProtocol(fLinkableId);
		boolean dirty = true;
		
		dirty = lnk.hasChanged(CommonState.getIntTime());
		dirty |= (fMappings == null);
		
		if (dirty) {
			recomputeComponents(node, lnk);
		}
		
		fLastCheck = CommonState.getIntTime();
	}
	
	@SuppressWarnings("unchecked")
	private void recomputeComponents(Node n, GraphProtocol fgp) {
		Graph g = fgp.graph();
		SubgraphDecorator neighborhood = new SubgraphDecorator(g, true);
		neighborhood.setVertexList(g.getNeighbours(fgp.getId()));
		
		Map<Integer, Integer> components = fGa.tarjan(neighborhood);
		fMappings = new ArrayList [components.size()];
		
		for (int i = 0; i < fMappings.length; i++) {
			fMappings[i] = new ArrayList<Integer>();
		}

		HashMap<Integer, Integer> roots = new HashMap<Integer, Integer>();
		int componentId = 0;
		for (int i = 0; i < neighborhood.size(); i++) {
			int color = fGa.root[i];
			if (roots.containsKey(color)) {
				color = roots.get(color);
			} else {
				roots.put(color, componentId);
				color = componentId++;
			}
			
			int originalId = neighborhood.inverseIdOf(i);
			fMappings[color].add(originalId);
			fComponentTable.put(originalId, componentId);
		}
	}
	
	@SuppressWarnings("unchecked")
	public Object clone() {
		try {
			ComponentComputationService cssClone = (ComponentComputationService) super.clone();
			if (fMappings != null) {
				cssClone.fMappings = new ArrayList [fMappings.length];
				for (int i = 0; i < fMappings.length; i++) {
					cssClone.fMappings[i] = new ArrayList<Integer>(fMappings[i]);
				}
			}
			cssClone.fComponentTable = new HashMap(this.fComponentTable);		
			return cssClone;
		} catch (CloneNotSupportedException ex) {
			throw new RuntimeException();
		}
	}

}
