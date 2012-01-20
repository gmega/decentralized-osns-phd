package it.unitn.disi.unitsim.experiments;

import java.util.ArrayList;
import java.util.Arrays;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Fallible;
import peersim.util.IncrementalStats;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.ed.IEDUnitExperiment;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.INodeStateListener;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.ITableWriter;

@AutoConfig
@StructuredLog(key = "TCR", fields = { "id", "distance", "perceived_latency",
		"first_login_latency" })
public class SimplePathExperiment extends GraphExperiment implements
		INodeStateListener, IEDUnitExperiment {

	private ArrayList<IExperimentObserver<IEDUnitExperiment>> fObservers = new ArrayList<IExperimentObserver<IEDUnitExperiment>>();

	private boolean fTerminated;

	private long[] fReachedUptime;

	private long[] fBaseUptime;

	private long[] fTTC;

	private IncrementalStats[] fTTCStats;

	private int fReachedCount = 0;

	private int fRepeats;

	private boolean fWait = true;

	private BurnInSupport fBurnin;

	private ITableWriter fLog;

	private ProgressTracker fTracker;

	public SimplePathExperiment(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("id") Integer id,
			@Attribute("linkable") int graphProtocolId,
			@Attribute("repeats") int repeats, @Attribute("burnin") int burnin,
			@Attribute("NeighborhoodLoader") IGraphProvider loader,
			@Attribute("TabularLogManager") TabularLogManager manager) {
		super(prefix, id, graphProtocolId, loader);
		fRepeats = repeats;
		fBurnin = new BurnInSupport(burnin, id);
		fLog = manager.get(SimplePathExperiment.class);
		fTracker = Progress.newTracker("experiment", fRepeats);
		fTracker.startTask();
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
	public boolean isTimedOut() {
		return false;
	}

	// ------------------------------------------------------------------------

	@Override
	public void stateChanged(int oldState, int newState, SNNode node) {

		// During burn-in we ignore all events.
		if (fBurnin.isBurningIn()) {
			return;
		}

		// Has node 0 already made its first renewal?
		if (!clearToStart(newState, node) || newState != Fallible.OK) {
			return;
		}

		// Proceeds by induction.
		for (int i = 1; i < fReachedUptime.length; i++) {
			if (reached(i)) {
				continue;
			}

			if (getNode(i - 1).isUp() && getNode(i).isUp()) {
				mark(i);
				continue;
			}

			break;
		}

		if (fReachedCount == fReachedUptime.length) {
			startNext();
		}
	}

	// ------------------------------------------------------------------------

	@Override
	protected void chainInitialize() {
		fReachedUptime = new long[size()];
		fTTC = new long[size()];
		fBaseUptime = new long[size()];
		fTTCStats = new IncrementalStats[size()];
		for (int i = 0; i < size(); i++) {
			fTTCStats[i] = new IncrementalStats();
			getNode(i).setStateListener(this);
		}
	}

	// ------------------------------------------------------------------------

	private void clearState() {
		Arrays.fill(fReachedUptime, Long.MIN_VALUE);
		Arrays.fill(fTTC, Long.MIN_VALUE);
		Arrays.fill(fBaseUptime, Long.MIN_VALUE);
		fReachedCount = 0;
	}

	// ------------------------------------------------------------------------

	private void startNext() {
		for (int i = 0; i < fReachedUptime.length; i++) {
			// Prints statistics.
			fLog.set("id", getId());
			fLog.set("distance", i);
			fLog.set("first_login_latency", fTTC[i] - fTTC[0]);
			fLog.set("perceived_latency", fReachedUptime[i] - fBaseUptime[i]);
			fLog.emmitRow();
			fTTCStats[i].add(fTTC[i] - fTTC[0]);
		}
		fRepeats--;
		fTracker.tick();

		if (fRepeats == 0) {
			fTracker.done();
			for (int i = 0; i < fTTCStats.length; i++) {
				System.out.println("AVG:" + size() + " " + i + " "
						+ fTTCStats[i].getSum() + " " + fTTCStats[i].getN()
						+ " " + fTTCStats[i].getAverage());
			}

			for (int i = 0; i < size(); i++) {
				SNNode n = getNode(i);
				System.out.println("ND:" + i + " " + n.uptime(false) + " "
						+ n.uptimeN(false) + " " + n.downtime(false) + " "
						+ n.downtimeN(false));
			}

			done();
		} else {
			fWait = true;
		}

	}

	/**
	 * We are clear to start when the first node comes on-line for the "first"
	 * time. In event terms, this simply means we need to hear a shift from off
	 * to on from node zero -- this means it has just started a renewal.
	 * 
	 * @param newState
	 *            {@link Fallible} state of the node.
	 * @param node
	 *            the node shifting state.
	 * @return <code>true</code> if we still have to wait before running the
	 *         experiment, or <code>false</code> otherwise.
	 */
	private boolean clearToStart(int newState, SNNode node) {

		if (!fWait) {
			return true;
		}

		if (node.getID() == 0 && newState == Fallible.OK) {
			clearState();
			fWait = false;
			mark(0);
			// Snapshots everyone's uptime to start counting
			// latency.
			for (int i = 0; i < size(); i++) {
				fBaseUptime[i] = getNode(i).uptime(true);
			}
		}

		return false;
	}

	// ------------------------------------------------------------------------

	private void mark(int index) {
		fReachedUptime[index] = getNode(index).uptime(true);
		fTTC[index] = ellapsedTime();
		fReachedCount++;
	}

	// ------------------------------------------------------------------------

	private boolean reached(int i) {
		return fReachedUptime[i] != Long.MIN_VALUE;
	}

	// ------------------------------------------------------------------------

	@Override
	public void interruptExperiment() {
		fTerminated = true;
		killAll();
		for (IExperimentObserver<IEDUnitExperiment> observer : fObservers) {
			observer.experimentEnd(this);
		}
	}

	// ------------------------------------------------------------------------

	@Override
	public void addObserver(IExperimentObserver<IEDUnitExperiment> observer) {
		fObservers.add(observer);
	}

}
