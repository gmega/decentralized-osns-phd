package it.unitn.disi.unitsim.experiments;

import java.util.Arrays;
import java.util.Stack;

import it.unitn.disi.unitsim.NeighborhoodLoader;
import it.unitn.disi.utils.peersim.INodeRegistry;
import it.unitn.disi.utils.peersim.SNNode;
import peersim.config.Attribute;
import peersim.core.CommonState;

public class TemporalConnectivityExperiment extends NeighborhoodExperiment {

	public static final int UNREACHABLE = Integer.MAX_VALUE;
	
	public static enum Mode {
		uptime, total;
	}
	
	// ------------------------------------------------------------------------

	private final Mode fMode;

	private final int fHorizon;

	private final int fTimeout;

	private final int fStartingTime;
	
	private final int fTimeBase;

	private final Stack<DFSFrame> fStack = new Stack<DFSFrame>();

	private final INodeRegistry fRegistry;

	private int fReached;

	private int[][] fReachabilityTable;

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
			@Attribute("distanceType") String mode) {
		super(prefix, id, graphProtocolId, loader);

		fTimeout = timeout;
		fHorizon = horizon <= 0 ? UNREACHABLE : horizon;
		fStartingTime = CommonState.getIntTime();
		fRegistry = registry;
		fMode = Mode.valueOf(mode.toLowerCase());
		fTimeBase = timeBase;
	}

	// ------------------------------------------------------------------------

	@Override
	protected void chainInitialize() {
		int dim = graph().size();
		// The reachability matrix contains the number of steps it takes
		// to reach a node from any other node.
		fReachabilityTable = new int[dim][];
		fRoots = new boolean[dim];
		for (int i = 0; i < dim; i++) {
			fReachabilityTable[i] = new int[dim];
			Arrays.fill(fReachabilityTable[i], UNREACHABLE);
			fReached++;
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public boolean cycled() {
		for (int i = 0; i < fReachabilityTable.length; i++) {
			explore(i);
		}

		return fReached == (fReachabilityTable.length * fReachabilityTable.length)
				|| ellapsedTime() == fTimeout;
	}

	// ------------------------------------------------------------------------

	@Override
	public void done() {
	}

	// ------------------------------------------------------------------------

	private int ellapsedTime() {
		return CommonState.getIntTime() - fStartingTime;
	}

	// ------------------------------------------------------------------------

	/**
	 * Main algorithm for the connectivity experiments.
	 */
	private void explore(int i) {
		
		int[] reachedTable = fReachabilityTable[i];
		SNNode root = (SNNode) fRegistry.getNode((long) i);
		if (reachedTable[i] == UNREACHABLE) {
			if (!root.isUp()) {
				return;
			} else {
				reachedTable[i] = runningTime(root);
			}
		}
		
		// Computes nodes that are to be explored.
		Arrays.fill(fRoots, false);
				
		for (int j = 0; j < reachedTable.length; j++) {
			SNNode node = (SNNode) fRegistry.getNode((long) j);
			fRoots[j] = (reachedTable[j] != UNREACHABLE) && node.isUp();
		}

		// For each already reached node...
		for (int j = 0; j < reachedTable.length; j++) {
			// ... expands the reachability.
			if (fRoots[j]) {
				recomputeReachability(j, reachedTable);
			}
		}
	}

	// ------------------------------------------------------------------------

	private void recomputeReachability(int root, int[] reached) {
		fStack.push(new DFSFrame(root));
		while (!fStack.isEmpty()) {
			DFSFrame frame = fStack.peek();
			// Explores while there are neighbors and we're below the horizon.
			if (frame.hasNext() && fStack.size() <= fHorizon) {
				int neighbor = frame.nextNeighbor();
				// Only pushes if node unmarked and reachable.
				SNNode neighborNode = (SNNode) fRegistry
						.getNode((long) neighbor);
				if (neighborNode.isUp()
						&& reached[neighbor] == UNREACHABLE) {
					fStack.push(new DFSFrame(neighbor));
					reached[neighbor] = runningTime(neighborNode);
				}
			}
			// Otherwise pops, and does nothing since we visit the node on push.
			else {
				fStack.pop();
			}
		}
	}
	
	// ------------------------------------------------------------------------
	
	public int nodes() {
		return fReachabilityTable.length;
	}
	
	// ------------------------------------------------------------------------

	public int reachability(int i, int j) {
		return fReachabilityTable[i][j];
	}
	
	// ------------------------------------------------------------------------

	public int runningTime(SNNode neighborNode) {
		int delta = 0;
		switch (fMode) {
		case total:
			delta = CommonState.getIntTime() - fStartingTime;
			break;
		case uptime:
			delta = (int) neighborNode.uptime();
			break;
		default:
			throw new IllegalStateException("Invalid mode " + fMode + ".");
		}
		return fTimeBase + delta;
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
