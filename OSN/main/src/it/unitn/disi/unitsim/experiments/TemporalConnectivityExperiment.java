package it.unitn.disi.unitsim.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.NeighborhoodLoader;
import it.unitn.disi.unitsim.ed.IEDUnitExperiment;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.INodeStateListener;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.ITableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.core.Linkable;

@AutoConfig
@StructuredLog(key = "TCE", fields = { "root", "degree", "reached", "total", "time" })
public class TemporalConnectivityExperiment extends NeighborhoodExperiment
		implements IEDUnitExperiment, INodeStateListener {

	public static final int UNREACHABLE = Integer.MAX_VALUE;

	// ------------------------------------------------------------------------

	private final int fHorizon;

	private final int fTimeout;

	private final int fStartingTime;

	private final int fTimeBase;

	private final Stack<DFSFrame> fStack = new Stack<DFSFrame>();

	private final ArrayList<IExperimentObserver> fObservers = new ArrayList<IExperimentObserver>();

	private final INodeRegistry fRegistry;

	private final ITableWriter fWriter;

	private int fReached;

	private int[][] fTotalReachability;

	private int[][] fUptimeReachability;

	private boolean[] fRoots;

	// ------------------------------------------------------------------------

	public TemporalConnectivityExperiment(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("id") Integer id,
			@Attribute("linkable") int graphProtocolId,
			@Attribute("NeighborhoodLoader") NeighborhoodLoader loader,
			@Attribute(value = "timebase", defaultValue = "0") int timeBase,
			@Attribute("timeout") int timeout,
			@Attribute(value = "horizon", defaultValue = "-1") int horizon,
			@Attribute("NodeRegistry") INodeRegistry registry,
			@Attribute("TabularLogManager") TabularLogManager manager) {
		this(prefix, id, graphProtocolId, loader, timeBase, timeout, horizon,
				registry, manager.get(TemporalConnectivityExperiment.class));
	}

	// ------------------------------------------------------------------------

	public TemporalConnectivityExperiment(String prefix, Integer id,
			int graphProtocolId, NeighborhoodLoader loader, int timeBase,
			int timeout, int horizon, INodeRegistry registry,
			ITableWriter writer) {
		super(prefix, id, graphProtocolId, loader);
		fTimeout = timeout;
		fHorizon = horizon <= 0 ? UNREACHABLE : horizon;
		fStartingTime = CommonState.getIntTime();
		fRegistry = registry;
		fTimeBase = timeBase;
		fWriter = writer;
	}

	// ------------------------------------------------------------------------

	@Override
	protected void chainInitialize() {
		int dim = graph().size();
		fRoots = new boolean[dim];
		fTotalReachability = newTable(dim);
		fUptimeReachability = newTable(dim);

		rootNode().setStateListener(this);
		Linkable neighborhood = neighborhood();
		int degree = neighborhood.degree();
		for (int i = 0; i < degree; i++) {
			SNNode neighbor = (SNNode) neighborhood.getNeighbor(i);
			neighbor.setStateListener(this);
		}
	}

	// ------------------------------------------------------------------------

	private int[][] newTable(int dim) {
		int[][] table = new int[dim][];
		for (int i = 0; i < dim; i++) {
			table[i] = new int[dim];
			Arrays.fill(table[i], UNREACHABLE);
		}
		return table;
	}

	// ------------------------------------------------------------------------

	@Override
	public void stateChanged(int oldState, int newState, SNNode node) {
		if (newState == Fallible.DEAD) {
			return;
		}
		
		for (int i = 0; i < fTotalReachability.length; i++) {
			explore(i);
		}

		if (ellapsedTime() == fTimeout) {
			System.err.println("-- Timeout!");
			terminate();
		}
		
		if (fReached == total()) {
			System.err.println("-- All nodes reached.");
			terminate();
		}
	}

	private int total() {
		return (fTotalReachability.length * fTotalReachability.length);
	}

	// ------------------------------------------------------------------------

	@Override
	public void done() {
		System.err.println("-- Done: " + rootNode().getSNId() + ".");
	}

	// ------------------------------------------------------------------------

	private int ellapsedTime() {
		return CommonState.getIntTime() - fStartingTime;
	}
	
	// ------------------------------------------------------------------------
	
	private void terminate() {
		for(IExperimentObserver observer : fObservers) {
			observer.experimentEnd(this);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Main algorithm for the connectivity experiments.
	 */
	private void explore(int i) {

		SNNode root = (SNNode) fRegistry.getNode((long) i);
		if (!isReachable(i, i)) {
			if (!root.isUp()) {
				return;
			} else {
				updateRunningTimes(i, root);
			}
		}

		// Computes nodes that are to be explored.
		Arrays.fill(fRoots, false);

		for (int j = 0; j < dim(); j++) {
			SNNode node = (SNNode) fRegistry.getNode((long) j);
			fRoots[j] = (isReachable(i, j)) && node.isUp();
		}

		// For each already reached node...
		for (int j = 0; j < dim(); j++) {
			// ... expands the reachability.
			if (fRoots[j]) {
				recomputeReachability(i, j);
			}
		}
	}

	// ------------------------------------------------------------------------

	private void recomputeReachability(int root, int infected) {
		DFSFrame current = new DFSFrame(infected);
		while (true) {
			// Explores while there are neighbors and we're below the horizon.
			if (current.hasNext() && fStack.size() <= fHorizon) {
				int neighbor = current.nextNeighbor();
				// Only pushes if node unmarked and up.
				SNNode neighborNode = (SNNode) fRegistry
						.getNode((long) neighbor);
				if (neighborNode.isUp() && !isReachable(root, neighbor)) {
					fStack.push(current);
					current = new DFSFrame(neighbor);
					updateRunningTimes(root, neighborNode);
				}
			}
			// Otherwise pops, and does nothing since we visit the node on push.
			else {
				if (fStack.isEmpty()) {
					break;
				}
				current = fStack.pop();
			}
		}
	}

	// ------------------------------------------------------------------------

	private boolean isReachable(int root, int node) {
		return fTotalReachability[root][node] != UNREACHABLE;
	}

	// ------------------------------------------------------------------------

	public int dim() {
		return fTotalReachability.length;
	}

	// ------------------------------------------------------------------------

	public int reachability(int i, int j) {
		return fTotalReachability[i][j];
	}

	// ------------------------------------------------------------------------

	private void updateRunningTimes(int root, SNNode neighborNode) {
		int idx = (int) neighborNode.getID();
		fTotalReachability[root][idx] = fTimeBase + CommonState.getIntTime()
				- fStartingTime;
		fUptimeReachability[root][idx] = fTimeBase
				+ (int) neighborNode.uptime();
		fReached++;

		fWriter.set("root", rootNode().getID());
		fWriter.set("degree", neighborhood().degree());
		fWriter.set("reached", fReached);
		fWriter.set("total", total());
		fWriter.set("time", ellapsedTime());
		fWriter.emmitRow();
	}

	// ------------------------------------------------------------------------

	@Override
	public void addObserver(IExperimentObserver observer) {
		fObservers.add(observer);
	}

	// ------------------------------------------------------------------------

	class DFSFrame {

		private int fNode;
		private int fIndex = 0;
		private int fDegree;

		public DFSFrame(int node) {
			fNode = node;
			fDegree = graph().degree(node);
		}

		public boolean hasNext() {
			return fIndex < fDegree;
		}

		public int nextNeighbor() {
			return graph().getNeighbor(fNode, fIndex++);
		}
	}

	// ------------------------------------------------------------------------
}
