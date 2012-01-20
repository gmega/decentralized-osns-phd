package it.unitn.disi.unitsim.experiments;

import java.util.ArrayList;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.CommonState;
import peersim.core.Fallible;
import peersim.util.IncrementalStats;
import it.unitn.disi.graph.large.catalog.IGraphProvider;
import it.unitn.disi.unitsim.IExperimentObserver;
import it.unitn.disi.unitsim.ed.IEDUnitExperiment;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.INodeStateListener;
import it.unitn.disi.utils.peersim.SNNode;

@AutoConfig
@StructuredLog(key = "TCR:", fields = { "id", "distance", "perceived_latency",
		"first_login_latency" })
public class SyncDesyncExperiment extends GraphExperiment implements
		INodeStateListener, IEDUnitExperiment {

	static final byte S00 = 0x00;
	static final byte S01 = 0x01;
	static final byte S10 = 0x02;
	static final byte S11 = 0x03;

	private ArrayList<IExperimentObserver<IEDUnitExperiment>> fObservers = new ArrayList<IExperimentObserver<IEDUnitExperiment>>();

	private boolean fTerminated;

	private long fDesyncStart = -1;

	private int fZeros;

	private int fRepeats;

	private IncrementalStats fIntersync = new IncrementalStats();

	public SyncDesyncExperiment(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("id") Integer id,
			@Attribute("linkable") int graphProtocolId,
			@Attribute("NeighborhoodLoader") IGraphProvider loader,
			@Attribute("repetitions") int repetitions,
			@Attribute("TabularLogManager") TabularLogManager manager) {
		super(prefix, id, graphProtocolId, loader);
		fRepeats = repetitions;
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
		for (int i = 0; i < size(); i++) {
			getNode(i).setStateListener(this);
		}
	}

	@Override
	public void stateChanged(int oldState, int newState, SNNode node) {
		// Process sync changes.
		byte state = syncState();

		if (state == S10) {
			if (!inDesynchEpoch()) {
				startDesyncEpoch();
			}
		}

		if (state == S11) {
			endDesynchEpoch();
		}

	}

	private void endDesynchEpoch() {
		if (!inDesynchEpoch()) {
			fZeros++;
		} else {
			fIntersync.add(CommonState.getTime() - fDesyncStart);
			fDesyncStart = -1;
		}

		fRepeats--;
		if (fRepeats == 0) {
			done();
		}
	}

	private void startDesyncEpoch() {
		fDesyncStart = CommonState.getTime();
	}

	private boolean inDesynchEpoch() {
		return fDesyncStart > 0;
	}

	@Override
	public void interruptExperiment() {
		fTerminated = true;
		killAll();
		for (IExperimentObserver<IEDUnitExperiment> observer : fObservers) {
			observer.experimentEnd(this);
		}

		System.out.println("EH: isum in zeros iavg");
		System.out.println("E:" + fIntersync.getSum() + " " + fIntersync.getN()
				+ " " + fZeros + " " + fIntersync.getAverage());
	}

	private byte syncState() {
		byte state = 0;
		if (getNode(0).isUp()) {
			state |= 2;
		}
		if (getNode(1).isUp()) {
			state |= 1;
		}

		return state;
	}

	@Override
	public void addObserver(IExperimentObserver<IEDUnitExperiment> observer) {
		fObservers.add(observer);
	}

}
