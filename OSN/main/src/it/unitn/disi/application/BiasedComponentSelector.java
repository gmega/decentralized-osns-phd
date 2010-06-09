package it.unitn.disi.application;

import it.unitn.disi.IDynamicLinkable;
import it.unitn.disi.util.RouletteWheel;
import it.unitn.disi.utils.graph.SubgraphDecorator;

import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Linkable;
import peersim.core.Node;
import peersim.core.OverlayGraph;
import peersim.graph.Graph;
import peersim.graph.GraphAlgorithms;

/**
 * {@link BiasedComponentSelector} is a {@link Linkable} which, at each cycle,
 * picks one connected subcomponent of the 1-hop neighborhood of another
 * {@link Linkable} to be the actual set of neighbors.
 * 
 * Components are chosen in a Roulette-wheel style selection (see
 * {@link RouletteWheel}), with larger components being given preference.
 * 
 * @author giuliano
 */
public class BiasedComponentSelector implements IDynamicLinkable, CDProtocol {

	private static final String PAR_SNID = "linkable";

	private GraphAlgorithms fGa = new GraphAlgorithms();

	private int fSnId;

	private Integer[] fRootIds;

	private Integer[] fSizes;

	private int fComponentCount;

	private Node[] fSelected;

	private RouletteWheel fWheel;

	private Graph fGraph;

	private int fChangeStamp = -1;

	private Random fRandom;

	private boolean fTestMode;

	public BiasedComponentSelector(String name) {
		this(Configuration.getPid(name + "." + PAR_SNID), CommonState.r, null,
				false);
	}

	public BiasedComponentSelector(int snId, Random random, Graph graph,
			boolean testMode) {
		fSnId = snId;
		fRandom = random;
		fGraph = graph;
		fTestMode = testMode;
	}
	
	public void nextCycle(Node source, int protocolId) {
		updateSelection(source);
	}

	public void updateSelection(Node source) {
		Linkable linkable = (Linkable) source.getProtocol(fSnId);
		if (hasChanged(linkable)) {
			Map<Integer, Integer> components = recomputeComponents(source);

			fComponentCount = components.size();
			fRootIds = resize(fRootIds, fComponentCount);
			fSizes = resize(fSizes, fComponentCount);

			Iterator<Integer> it = components.keySet().iterator();
			for (int i = 0; it.hasNext(); i++) {
				fRootIds[i] = it.next();
				fSizes[i] = components.get(fRootIds[i]);
			}

			fWheel = createWheel(components, fRootIds, fComponentCount);
		}

		int idx = fWheel.spin();
		fSelected = extractComponent(fRootIds[idx], fSizes[idx]);

		changed();
	}

	private void changed() {
		if (!fTestMode) {
			fChangeStamp = CommonState.getIntTime();
		}
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, Integer> recomputeComponents(Node source) {
		// Starts by creating an overlay graph over the social network linkable.
		Graph g = getGraph();
		SubgraphDecorator decorator = new SubgraphDecorator(g, true);
		decorator.setVertexList(g.getNeighbours((int) source.getID()));

		return fGa.tarjan(decorator);
	}

	private Node[] extractComponent(int rootId, int size) {
		Node[] component = new Node[size];
		Graph g = getGraph();

		int j = 0;
		for (int i = 0; i < fGa.root.length; i++) {
			if (fGa.root[i] == rootId) {
				component[j++] = (Node) g.getNode(i);
			}
		}

		return component;
	}

	private Graph getGraph() {
		if (fGraph == null) {
			fGraph = new OverlayGraph(fSnId);
		}

		return fGraph;
	}

	private RouletteWheel createWheel(Map<Integer, Integer> id2size,
			Integer[] rootIds, int size) {
		double[] probabilities = new double[size];

		// Assuming here that id2size.size() == size.
		int totalSize = 0;
		for (Integer componentSize : id2size.values()) {
			totalSize += componentSize;
		}

		for (int i = 0; i < size; i++) {
			probabilities[i] = (id2size.get(rootIds[i]) / ((double) totalSize));
		}

		return new RouletteWheel(probabilities, fRandom);
	}

	private boolean hasChanged(Linkable linkable) {
		boolean changed = true;
		if (linkable instanceof IDynamicLinkable && !fTestMode) {
			try {
				changed = ((IDynamicLinkable) linkable).hasChanged(CommonState
						.getIntTime());
			} catch (ExceptionInInitializerError ex) {
				// Swallow.
				ex.printStackTrace();
			}
		}

		// Needless to say, IDynamicLinkables should be used
		// for anything but the smallest simulations.
		return changed || (fSelected == null);
	}

	private Integer[] resize(Integer[] array, int size) {
		if (array == null || array.length < size) {
			array = new Integer[size];
		}

		return array;
	}

	public Object clone() {
		try {
			BiasedComponentSelector clone = (BiasedComponentSelector) super
					.clone();

			if (fSelected != null) {
				clone.fSelected = new Node[fSelected.length];
				System.arraycopy(fSelected, 0, clone.fSelected, 0,
						fSelected.length);
			}

			if (fRootIds != null) {
				clone.fRootIds = new Integer[fRootIds.length];
				System.arraycopy(fRootIds, 0, clone.fRootIds, 0,
						fRootIds.length);
			}

			if (fSizes != null) {
				clone.fSizes = new Integer[fSizes.length];
				System.arraycopy(fSizes, 0, clone.fSizes, 0, fSizes.length);
			}

			if (fWheel != null) {
				clone.fWheel = (RouletteWheel) fWheel.clone();
			}
			
			clone.fGa = new GraphAlgorithms();
			clone.fGraph = null;
			
			return clone;
		} catch (CloneNotSupportedException ex) {
			// Just bubles it up.
			throw new RuntimeException(ex);
		}
	}

	public boolean hasChanged(int time) {
		return time > fChangeStamp;
	}

	public boolean contains(Node neighbor) {
		for (Node node : fSelected) {
			if (node == neighbor) {
				return true;
			}
		}
		return false;
	}

	public int degree() {
		return fSelected.length;
	}

	public Node getNeighbor(int i) {
		return fSelected[i];
	}

	public boolean addNeighbor(Node neighbour) {
		throw new UnsupportedOperationException();
	}

	public void pack() {
	}

	public void onKill() {
	}
}
