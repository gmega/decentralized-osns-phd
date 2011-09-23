package it.unitn.disi.unitsim.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import it.unitn.disi.graph.lightweight.LightweightStaticGraph;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.NeighborhoodLoader;
import it.unitn.disi.unitsim.ed.IEDUnitExperiment;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.collections.Pair;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.StructuredLogs;
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
import peersim.core.Node;

@AutoConfig
@StructuredLogs({
		@StructuredLog(key = "TCP", fields = { "root", "degree", "reached",
				"total", "time" }),
		@StructuredLog(key = "TCS", fields = { "root", "originator", "total",
				"reached", "residue", "corrected", "pathological" }),
		@StructuredLog(key = "TCR", fields = { "root", "originator",
				"receiver", "total_length", "uptime_length",
				"first_uptime_length" }),
		@StructuredLog(key = "TCE", fields = { "root", "time", "residue",
				"corrected" }) })
public class TemporalConnectivityExperiment extends NeighborhoodExperiment
		implements IEDUnitExperiment, INodeStateListener {

	public static final int NEVER = Integer.MAX_VALUE;

	// ------------------------------------------------------------------------
	// Experiment configuration parameters.
	// ------------------------------------------------------------------------

	/**
	 * Time interval at the beginning of the experiment for which no temporal
	 * connectivity measurements will be made. This is mostly so that
	 * availability patterns can settle in their stable state.
	 */
	private final long fBurnInTime;

	/**
	 * An offset to be added to all time measurements. In most cases, set to 1
	 * or 0 for 1 or zero-based time.
	 */
	private final long fTimeBase;

	/**
	 * A timeout value after which the experiment will cease.
	 */
	private final long fTimeout;

	/**
	 * If set to true, resets the timeout of each connectivity measurement
	 * whenever there is progress.
	 */
	private final int fHorizon;

	// ------------------------------------------------------------------------
	// Data structures.
	// ------------------------------------------------------------------------

	private final Stack<DFSFrame> fStack = new Stack<DFSFrame>();

	private final ArrayList<IExperimentObserver<IEDUnitExperiment>> fObservers = new ArrayList<IExperimentObserver<IEDUnitExperiment>>();

	private boolean[][] fReachability;

	private boolean[] fRoots;

	private int[][] fFirstUptime;

	private boolean fBurningIn;

	// ------------------------------------------------------------------------
	// References to services.
	// ------------------------------------------------------------------------

	private final INodeRegistry fRegistry;

	private final ITableWriter fProgressWriter;

	private final ITableWriter fReachabilityWriter;

	private final ITableWriter fSummaryWriter;

	private final ITableWriter fResidueWriter;

	// ------------------------------------------------------------------------
	// Other state variables.
	// ------------------------------------------------------------------------

	private int fReached;

	private boolean fTerminated = false;

	// ------------------------------------------------------------------------

	public TemporalConnectivityExperiment(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("id") Integer id,
			@Attribute("linkable") int graphProtocolId,
			@Attribute("NeighborhoodLoader") NeighborhoodLoader loader,
			@Attribute(value = "timebase", defaultValue = "0") int timeBase,
			@Attribute(value = "horizon", defaultValue = "-1") int horizon,
			@Attribute(value = "timeout", defaultValue = "0") long timeout,
			@Attribute(value = "burnin", defaultValue = "0") int burnIn,
			@Attribute("NodeRegistry") INodeRegistry registry,
			@Attribute("TabularLogManager") TabularLogManager manager) {
		this(prefix, id, graphProtocolId, loader, timeBase, horizon, timeout,
				burnIn, registry, manager.get(
						TemporalConnectivityExperiment.class, "TCP"), manager
						.get(TemporalConnectivityExperiment.class, "TCR"),
				manager.get(TemporalConnectivityExperiment.class, "TCE"),
				manager.get(TemporalConnectivityExperiment.class, "TCS"));
	}

	// ------------------------------------------------------------------------

	public TemporalConnectivityExperiment(String prefix, Integer id,
			int graphProtocolId, NeighborhoodLoader loader, int timeBase,
			int horizon, long timeout, int burnIn, INodeRegistry registry,
			ITableWriter progressWriter, ITableWriter reachabilityWriter,
			ITableWriter summaryWriter, ITableWriter residueWriter) {
		super(prefix, id, graphProtocolId, loader);
		fHorizon = horizon <= 0 ? Integer.MAX_VALUE : horizon;
		fRegistry = registry;
		fTimeBase = timeBase;
		fTimeout = timeout == 0 ? Long.MAX_VALUE : timeout;
		
		fBurnInTime = burnIn;
		if (burnIn > 0) {
			System.err.println("-- Start burn-in period for experiment "
					+ getId() + ".");
			fBurningIn = true;
		}

		fReachabilityWriter = reachabilityWriter;
		fProgressWriter = progressWriter;
		fSummaryWriter = summaryWriter;
		fResidueWriter = residueWriter;
	}

	// ------------------------------------------------------------------------

	@Override
	protected void chainInitialize() {
		int dim = graph().size();
		fRoots = new boolean[dim];
		fReachability = new boolean[dim][];
		fFirstUptime = new int[dim][];
		for (int i = 0; i < fReachability.length; i++) {
			fReachability[i] = new boolean[dim];
			
			// Initialize first uptimes.
			fFirstUptime[i] = new int[dim];
			Arrays.fill(fFirstUptime[i], NEVER);
			fFirstUptime[i][i] = 0;
		}

		rootNode().setStateListener(this);
		Linkable neighborhood = neighborhood();
		int degree = neighborhood.degree();
		for (int i = 0; i < degree; i++) {
			SNNode neighbor = (SNNode) neighborhood.getNeighbor(i);
			neighbor.setStateListener(this);
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public void stateChanged(int oldState, int newState, SNNode node) {

		if (!updateBurnIn()) {
			return;
		}

		switch (newState) {

		case Fallible.OK: {
			int id = (int) node.getID();
			updateSelfReachability(id);
			updateFirstUptimes(id);
			for (int i = 0; i < fReachability.length; i++) {
				explore(i);
			}
		}

		case Fallible.DOWN:
		case Fallible.DEAD:
			break;

		}

		if (fReached == total()) {
			System.err.println("-- All nodes reached.");
			finished();
		}
	}

	// ------------------------------------------------------------------------

	protected void updateFirstUptime(int sender, int receiver) {
		// Has the sender been up already?
		if (isReachable(sender, sender)) {
			// Do we know that already?
			if (fFirstUptime[sender][receiver] == NEVER) {
				System.err.println("First uptime of " + receiver + " to " + sender + " is " + ellapsedTime() + ".");
				// No, start ticking our clock.
				fFirstUptime[sender][receiver] = MiscUtils
						.safeCast(ellapsedTime());
			}
		}
	}

	// ------------------------------------------------------------------------

	private void updateFirstUptimes(int joining) {
		for (int i = 0; i < fReachability.length; i++) {
			// The joining node is a receiver...
			updateFirstUptime(i, joining);
			// ... but it's also a sender.
			SNNode other = (SNNode) fRegistry.getNode(i);
			if (other.isUp()) {
				updateFirstUptime(joining, i);
			}
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public void done() {
		if (!fTerminated) {
			interruptExperiment();
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public void interruptExperiment() {
		Pair<Double, Double> residues = printResidues();
		printSummary(residues);
		killAll();
		finished();
	}

	// ------------------------------------------------------------------------

	private boolean updateBurnIn() {
		if (!fBurningIn) {
			return true;
		}

		long ellapsed = ellapsedTime();
		if (ellapsed < fBurnInTime) {
			return false;
		}

		// Burn-in is over.
		// 1. Readjust the starting time.
		resetStartingTime();

		// 2. Zeroes all uptimes.
		rootNode().clearUptime();
		Linkable neighborhood = neighborhood();
		for (int i = 0; i < neighborhood.degree(); i++) {
			SNNode neighbor = (SNNode) neighborhood.getNeighbor(i);
			neighbor.clearUptime();
		}

		// 3. Updates self-reachability and uptimes for
		// everyone.
		for (int i = 0; i < dim(); i++) {
			Node node = fRegistry.getNode(i);
			if (node.isUp()) {
				updateSelfReachability(i);
				updateFirstUptimes(i);
			}
		}

		fBurningIn = false;

		System.err.println("-- Burn-in period for experiment " + getId()
				+ " over (excess was " + (ellapsed - fBurnInTime) + ").");

		return true;
	}

	// ------------------------------------------------------------------------

	private void finished() {
		fTerminated = true;
		for (IExperimentObserver<IEDUnitExperiment> observer : fObservers) {
			observer.experimentEnd(this);
		}
	}

	// ------------------------------------------------------------------------

	private void updateSelfReachability(int i) {
		SNNode root = (SNNode) fRegistry.getNode((long) i);
		if (!isReachable(i, i)) {
			reached(i, root);
		}
	}

	// ------------------------------------------------------------------------

	/**
	 * Main algorithm for the connectivity experiments.
	 */
	private void explore(int i) {
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
		LightweightStaticGraph lsg = (LightweightStaticGraph) graph();
		DFSFrame current = new DFSFrame(infected, lsg);
		while (true) {
			// Explores while there are neighbors and we're below the horizon.
			if (current.hasNext() && fStack.size() <= fHorizon) {
				int neighbor = current.nextNeighbor();
				// Only pushes if node unmarked and up.
				SNNode neighborNode = (SNNode) fRegistry
						.getNode((long) neighbor);
				if (neighborNode.isUp() && !isReachable(root, neighbor)) {
					fStack.push(current);
					current = new DFSFrame(neighbor, lsg);
					reached(root, neighborNode);
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
		return fReachability[root][node];
	}

	// ------------------------------------------------------------------------

	private void reached(int sender, SNNode reached) {
		int id = (int) reached.getID();
		// Updates reachability information.
		fReachability[sender][id] = true;
		fReached++;

		long uptime = reached.uptime();
		// Sanity test.
		if (fFirstUptime[sender][id] == NEVER) {
			throw new IllegalStateException("Event race condition detected.");
		}
		long fromFirst = ellapsedTime() - fFirstUptime[sender][id];
		
		printReachability(sender, reached.getSNId(),
				printableEllapsedTime(), fromFirst, uptime);
		printProgress();
	}

	// ------------------------------------------------------------------------

	public int dim() {
		return fReachability.length;
	}

	// ------------------------------------------------------------------------

	@Override
	public void addObserver(IExperimentObserver<IEDUnitExperiment> observer) {
		fObservers.add(observer);
	}

	// ------------------------------------------------------------------------

	@Override
	public boolean isTimedOut() {
		return !fBurningIn && ellapsedTime() >= fTimeout;
	}

	// ------------------------------------------------------------------------

	private void killAll() {
		Linkable neighborhood = neighborhood();
		int degree = neighborhood.degree();
		for (int i = 0; i < degree; i++) {
			Node node = neighborhood.getNeighbor(i);
			node.setFailState(Fallible.DEAD);
		}
		rootNode().setFailState(Fallible.DEAD);
	}

	// ------------------------------------------------------------------------

	private int total() {
		return (fReachability.length * fReachability.length);
	}

	// ------------------------------------------------------------------------

	private long printableEllapsedTime() {
		return fTimeBase + ellapsedTime();
	}

	// ------------------------------------------------------------------------

	protected long ellapsedTime() {
		return CommonState.getTime() - startTime();
	}

	// ------------------------------------------------------------------------

	protected void printProgress() {
		fProgressWriter.set("root", rootNode().getSNId());
		fProgressWriter.set("degree", neighborhood().degree());
		fProgressWriter.set("reached", fReached);
		fProgressWriter.set("total", total());
		fProgressWriter.set("time", printableEllapsedTime());
		fProgressWriter.emmitRow();
	}

	// ------------------------------------------------------------------------

	private void printReachability(long originator, long receiver, long total,
			long fromFirst, long uptime) {
		fReachabilityWriter.set("root", rootNode().getSNId());
		fReachabilityWriter.set("originator", originator);
		fReachabilityWriter.set("receiver", receiver);
		fReachabilityWriter.set("total_length", total);
		fReachabilityWriter.set("uptime_length", uptime);
		fReachabilityWriter.set("first_uptime_length", fromFirst);
		fReachabilityWriter.emmitRow();
	}

	// ------------------------------------------------------------------------

	private void printSummary(Pair<Double, Double> residues) {
		fSummaryWriter.set("root", rootNode().getSNId());
		fSummaryWriter.set("time", printableEllapsedTime());
		fSummaryWriter.set("residue", residues.a);
		fSummaryWriter.set("corrected", residues.b);
		fSummaryWriter.emmitRow();
	}

	// ------------------------------------------------------------------------

	private Pair<Double, Double> printResidues() {

		double totalReached = 0.0;
		double totalNonZero = 0.0;

		for (int i = 0; i < dim(); i++) {
			int reached = 0;
			int nonZeroUptime = 0;

			for (int j = 0; j < dim(); j++) {
				// Residue: has the node been reached?
				if (isReachable(i, j)) {
					reached++;
					totalReached += 1;
				}

				// Corrected residue: has the node come online?
				SNNode node = (SNNode) fRegistry.getNode((long) j);
				if (node.uptime() > 0) {
					nonZeroUptime++;
					totalNonZero += 1.0;
				}
			}

			double residue = 1.0 - (((double) reached) / dim());
			double corrected;
			boolean pathological = false;

			// Pathological case: issuing node hasn't come online.
			if (nonZeroUptime == 0) {
				corrected = 1.0;
				pathological = true;
			} else {
				corrected = 1.0 - (((double) reached) / nonZeroUptime);
			}

			SNNode originator = (SNNode) fRegistry.getNode((long) i);

			fResidueWriter.set("root", rootNode().getSNId());
			fResidueWriter.set("originator", originator.getSNId());
			fResidueWriter.set("reached", reached);
			fResidueWriter.set("total", dim());
			fResidueWriter.set("residue", residue);
			fResidueWriter.set("corrected", corrected);
			fResidueWriter.set("pathological", pathological);
			fResidueWriter.emmitRow();
		}

		return new Pair<Double, Double>(totalReached / (dim() * dim()),
				totalReached / totalNonZero);
	}

	// ------------------------------------------------------------------------

	static class DFSFrame {

		private int fNode;
		private int fIndex = 0;
		private int[] fNeighbors;

		public DFSFrame(int node, LightweightStaticGraph graph) {
			fNode = node;
			fNeighbors = graph.fastGetNeighbours(fNode);
		}

		public boolean hasNext() {
			return fIndex < fNeighbors.length;
		}

		public int nextNeighbor() {
			return fNeighbors[fIndex++];
		}
	}

	// ------------------------------------------------------------------------
}
