package it.unitn.disi.unitsim.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;

import it.unitn.disi.graph.GraphProtocol;
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
	 * connectivity measurements will be made. This is mostly used so that we
	 * can get availability patterns to settle in their stable state before we
	 * start measuring connectivity.
	 */
	private final long fBurnInTime;

	/**
	 * An offset to be added to all time measurements. In most cases, set to 1
	 * or 0 for 1 or zero-based time.
	 */
	private final long fTimeBase;

	/**
	 * A timeout value for the experiment. After fTimeout simulation time units
	 * have passed since the beginning of the experiment, calls to
	 * {@link #isTimedOut()} will start returning true.
	 */
	private final long fTimeout;

	/**
	 * The horizon is the number of hops that a "message" can traverse at each
	 * time step. Not setting it means instantaneous propagation.
	 */
	private final int fHorizon;

	// ------------------------------------------------------------------------
	// Data structures.
	// ------------------------------------------------------------------------

	private final Stack<DFSFrame> fStack = new Stack<DFSFrame>();

	private final ArrayList<IExperimentObserver<IEDUnitExperiment>> fObservers = new ArrayList<IExperimentObserver<IEDUnitExperiment>>();

	private SingleExperiment[] fExperiments;

	private boolean[] fRoots;

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

	private int fReachedCount;

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
		fExperiments = new SingleExperiment[dim];
		for (int i = 0; i < fExperiments.length; i++) {
			fExperiments[i] = new SingleExperiment(i);
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

		/* Interesting things only happen when nodes come up. */
		case Fallible.OK: {
			for (SingleExperiment exp : fExperiments) {
				exp.nodeUp(node);
			}

			for (int i = 0; i < dim(); i++) {
				explore(i);
			}

			if (fReachedCount == total()) {
				System.err.println("-- All nodes reached.");
				interruptExperiment();
			}

			break;
		}

		case Fallible.DOWN:
		case Fallible.DEAD:
			break;
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
		logSummary(residues);
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
			SNNode node = (SNNode) fRegistry.getNode(i);
			if (node.isUp()) {
				fExperiments[i].nodeUp(node);
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

	/**
	 * Main algorithm for the connectivity experiments.
	 */
	private void explore(int sender) {
		// Computes nodes that are to be explored.
		Arrays.fill(fRoots, false);

		for (int j = 0; j < dim(); j++) {
			SNNode node = (SNNode) fRegistry.getNode((long) j);
			fRoots[j] = fExperiments[sender].isReachable(j) && node.isUp();
		}

		// For each already reached node...
		for (int j = 0; j < dim(); j++) {
			// ... expands the reachability.
			if (fRoots[j]) {
				recomputeReachability(sender, j);
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
				if (neighborNode.isUp()
						&& !fExperiments[root].isReachable(neighbor)) {
					fStack.push(current);
					current = new DFSFrame(neighbor, lsg);
					fExperiments[root].reached(neighbor);
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

	private Pair<Double, Double> printResidues() {

		double totalReached = 0.0;
		double totalNonZero = 0.0;

		for (int i = 0; i < dim(); i++) {
			int reached = 0;
			int nonZeroUptime = 0;

			for (int j = 0; j < dim(); j++) {
				// Residue: has the node been reached?
				if (fExperiments[i].isReachable(j)) {
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

			logResidue(reached, residue, corrected, pathological, originator);
		}

		return new Pair<Double, Double>(totalReached / (dim() * dim()),
				totalReached / totalNonZero);
	}

	// ------------------------------------------------------------------------

	public int dim() {
		return fExperiments.length;
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
		return dim() * dim();
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

	protected void logProgress() {
		fProgressWriter.set("root", rootNode().getSNId());
		fProgressWriter.set("degree", neighborhood().degree());
		fProgressWriter.set("reached", fReachedCount);
		fProgressWriter.set("total", total());
		fProgressWriter.set("time", printableEllapsedTime());
		fProgressWriter.emmitRow();
	}

	// ------------------------------------------------------------------------

	private void logReached(long originator, long receiver, long total,
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

	private void logSummary(Pair<Double, Double> residues) {
		fSummaryWriter.set("root", rootNode().getSNId());
		fSummaryWriter.set("time", printableEllapsedTime());
		fSummaryWriter.set("residue", residues.a);
		fSummaryWriter.set("corrected", residues.b);
		fSummaryWriter.emmitRow();
	}

	// ------------------------------------------------------------------------

	private void logResidue(int reached, double residue, double corrected,
			boolean pathological, SNNode originator) {
		fResidueWriter.set("root", rootNode().getSNId());
		fResidueWriter.set("originator", originator.getSNId());
		fResidueWriter.set("reached", reached);
		fResidueWriter.set("total", dim());
		fResidueWriter.set("residue", residue);
		fResidueWriter.set("corrected", corrected);
		fResidueWriter.set("pathological", pathological);
		fResidueWriter.emmitRow();
	}

	// ------------------------------------------------------------------------

	class SingleExperiment {

		private final int[] fFirstLogon;

		private final boolean[] fReached;

		private int[] fUptimes;

		private final int fSender;

		private final int[] fNeighborhood;

		// ------------------------------------------------------------------------

		public SingleExperiment(int sender) {
			fSender = sender;

			fFirstLogon = new int[dim()];
			Arrays.fill(fFirstLogon, NEVER);

			fReached = new boolean[dim()];
			Arrays.fill(fReached, false);

			// Breaks encapsulation to yield better performance.
			GraphProtocol neighborhood = (GraphProtocol) neighborhood(fRegistry
					.getNode(sender));
			LightweightStaticGraph lsg = (LightweightStaticGraph) neighborhood
					.graph();
			fNeighborhood = lsg.fastGetNeighbours(fSender);
		}

		// ------------------------------------------------------------------------

		public void nodeUp(SNNode node) {
			if (node.getID() == fSender) {
				senderUp();
			} else {
				receiverUp(node);
			}
		}

		// ------------------------------------------------------------------------

		public boolean isReachable(int i) {
			return fReached[i];
		}

		// ------------------------------------------------------------------------

		public long uptimeOf(SNNode node) {
			return node.uptime() - fUptimes[(int) node.getID()];
		}

		// ------------------------------------------------------------------------

		public int firstLogonOf(SNNode node) {
			return fFirstLogon[(int) node.getID()];
		}

		// ------------------------------------------------------------------------

		public void reached(int id) {
			fReached[id] = true;
			reached((SNNode) fRegistry.getNode(id));
		}

		// ------------------------------------------------------------------------

		private void senderUp() {
			// This is not the first log-on event for the root, so
			// there's nothing to do.
			if (hasSenderPosted()) {
				return;
			}

			// Snapshots the uptimes of all nodes.
			if (fUptimes == null) {
				fUptimes = snapshotUptimes();
			}

			// Marks the root as reached.
			fReached[fSender] = true;
			fFirstLogon[fSender] = MiscUtils.safeCast(ellapsedTime());

			// Initializes the receiver map.
			for (int i = 0; i < fUptimes.length; i++) {
				SNNode node = (SNNode) fRegistry.getNode((long) i);
				if (node.isUp()) {
					receiverUp(node);
				}
			}

			reached((SNNode) fRegistry.getNode((long) fSender));
		}

		// ------------------------------------------------------------------------

		private void reached(SNNode reached) {
			fReachedCount++;
			int sender = (int) fSender;
			SingleExperiment exp = fExperiments[sender];
			long uptime = exp.uptimeOf(reached);
			long fromFirst = ellapsedTime() - exp.firstLogonOf(reached);

			assert fromFirst >= uptime;
			
			logReached(sender, reached.getSNId(), printableEllapsedTime(),
					fromFirst, uptime);
			logProgress();
		}

		// ------------------------------------------------------------------------

		private void receiverUp(SNNode node) {
			if (hasSenderPosted()) {
				updateFirstUptime((int) node.getID());
			}
		}

		// ------------------------------------------------------------------------

		private void updateFirstUptime(int id) {
			if (fFirstLogon[id] == NEVER) {
				fFirstLogon[id] = MiscUtils.safeCast(ellapsedTime());
			}
		}

		// ------------------------------------------------------------------------

		private int[] snapshotUptimes() {
			int[] snapshot = new int[dim()];
			for (int i = 0; i < dim(); i++) {
				snapshot[i] = MiscUtils
						.safeCast(((SNNode) fRegistry.getNode(i)).uptime());
			}
			return snapshot;
		}

		// ------------------------------------------------------------------------

		private boolean hasSenderPosted() {
			return fReached[fSender];
		}
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

}
