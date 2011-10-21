package it.unitn.disi.unitsim.experiments;

import java.util.ArrayList;
import java.util.Arrays;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Fallible;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.IGraphProvider;
import it.unitn.disi.unitsim.ed.IEDUnitExperiment;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.INodeStateListener;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.ITableWriter;

@AutoConfig
@StructuredLog(key = "TCR:", fields = { "id", "distance", "perceived_latency",
		"first_login_latency" })
public class SimplePathTemporalConnectivity extends GraphExperiment implements
		INodeStateListener, IEDUnitExperiment {

	private ArrayList<IExperimentObserver<IEDUnitExperiment>> fObservers = new ArrayList<IExperimentObserver<IEDUnitExperiment>>();

	private boolean fTerminated;

	private BurnInSupport fBurnin;

	private long[] fReached;
	
	private int fReachedCount = 0;
	
	private ITableWriter fLog;

	public SimplePathTemporalConnectivity(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("id") Integer id,
			@Attribute("linkable") int graphProtocolId,
			@Attribute("NeighborhoodLoader") IGraphProvider loader,
			@Attribute("burnin") int burnin,
			@Attribute("TabularLogManager") TabularLogManager manager) {
		super(prefix, id, graphProtocolId, loader);
		fBurnin = new BurnInSupport(burnin, id);
		fLog = manager.get(SimplePathTemporalConnectivity.class);
	}

	@Override
	public void done() {
		if (!fTerminated) {
			interruptExperiment();
		}
	}

	@Override
	public boolean isTimedOut() {
		return false;
	}

	@Override
	protected void chainInitialize() {
		fReached = new long[size()];
		Arrays.fill(fReached, Long.MIN_VALUE);
		for (int i = 0; i < size(); i++) {
			getNode(i).setStateListener(this);
		}
	}

	@Override
	public void stateChanged(int oldState, int newState, SNNode node) {
		if (fBurnin.isBurningIn()) {
			return;
		} else if (!fBurnin.wasBurningIn()) {
			// At the burn-in transition, we run the reachability
			// exploration regardless of the type of event we see.
			if (newState != Fallible.OK) {
				return;
			}
		}

		// Base case.
		if (node.getID() == 0 && !reached(0) && node.isUp()) {
			fReached[0] = 0;
			System.err.println("Reached 0.");
			fReachedCount++;
		}

		if (!reached(0)) {
			return;
		}

		// Proceeds by induction.
		for (int i = 1; i < fReached.length; i++) {
			if (reached(i)) {
				continue;
			}

			if (!reached(i) && getNode(i - 1).isUp() && getNode(i).isUp()) {
				fReached[i] = ((SNNode) getNode(i)).uptime();
				fReachedCount++;
				System.err.println("Reached " + i);
			} else {
				break;
			}
		}

		if (fReachedCount == fReached.length) {
			done();
		}
	}

	private boolean reached(int i) {
		return fReached[i] != Long.MIN_VALUE;
	}

	@Override
	public void interruptExperiment() {
		fTerminated = true;
		killAll();
		for (IExperimentObserver<IEDUnitExperiment> observer : fObservers) {
			observer.experimentEnd(this);
		}

		for (int i = 0; i < fReached.length; i++) {
			// Prints statistics.
			fLog.set("id", getId());
			fLog.set("distance", i);
			fLog.set("perceived_latency", fReached[i]);
			fLog.set("first_login_latency", -1);
			fLog.emmitRow();
		}
	}

	@Override
	public void addObserver(IExperimentObserver<IEDUnitExperiment> observer) {
		fObservers.add(observer);
	}

}
