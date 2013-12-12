package it.unitn.disi.churn.antientropy;

import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IReference;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.protocol.PeriodicAction;
import it.unitn.disi.utils.ResettableCounter;

import java.util.BitSet;
import java.util.Random;

public class Antientropy extends PeriodicAction {

	private static final long serialVersionUID = -7825255354837349538L;

	private final IPeerSelector fSelector;

	private final double fShortPeriod;

	private final double fLongPeriod;

	private final BitSet fSessionBlacklist;

	private final boolean fBlackList;

	private final IndexedNeighborGraph fGraph;

	private boolean fSuppressed;

	private ResettableCounter fShortRounds;

	private int fInitiate;

	private int fRespond;

	private int fPid;

	public Antientropy(IReference<ISimulationEngine> engine, Random rnd,
			IndexedNeighborGraph graph, int id, int prio,
			double shortPeriod, double longPeriod, int shortRounds,
			double initialDelay, boolean blacklist, IPeerSelector selector) {

		super(engine, prio, id, initialDelay);

		fShortPeriod = shortPeriod;
		fLongPeriod = longPeriod;
		
		fShortRounds = new ResettableCounter(shortRounds);

		fSelector = selector;
		fSessionBlacklist = new BitSet();
		fBlackList = blacklist;
		fGraph = graph;
	}

	@Override
	public void eventPerformed(ISimulationEngine state,
			Schedulable schedulable, double nextShift) {

		// Clear all per-session state.
		fSessionBlacklist.clear();
		fShortRounds.reset();
		fSuppressed = false;

		super.eventPerformed(state, schedulable, nextShift);
	}

	public int respond() {
		return fRespond;
	}

	public int initiate() {
		return fInitiate;
	}

	public void suppress() {
		fSuppressed = true;
	}

	@Override
	protected double performAction(ISimulationEngine engine) {
		// XXX this is not the most efficient way of implementing
		// suppression (though it is probably the simplest), as the periodic
		// action will be needlessly rescheduled.
		if (!fSuppressed) {
			doExchange(engine);
		}

		return engine.clock().rawTime()
				+ ((fShortRounds.get() > 0) ? fShortPeriod : fLongPeriod);
	}

	private void doExchange(ISimulationEngine engine) {
		INetwork network = engine.network();
		IProcess peer = selectPeer(network);

		// This antientropy implementation is really a hack: what it does is
		// to force a push protocol exchange, which in the end is okay if
		// we're only disseminating one message.
		if (peer != null) {
			this.fInitiate++;
			((Antientropy) peer.getProtocol(fPid)).fRespond++;
		}

		// Short rounds count even if we cannot select a peer.
		fShortRounds.decrement();
	}

	private IProcess selectPeer(INetwork network) {
		int neighbor = fSelector.selectPeer(id(), fGraph, fSessionBlacklist,
				network);

		if (neighbor < 0) {
			return null;
		}

		if (fBlackList) {
			fSessionBlacklist.set(neighbor);
		}

		return network.process(neighbor);
	}

}
