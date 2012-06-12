package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.IEdgeFilter;
import it.unitn.disi.simulator.core.EDSimulationEngine;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.INetworkMetric;
import it.unitn.disi.simulator.protocol.ICyclicProtocol;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.collections.Triplet;

import java.util.BitSet;

public class HFlood implements ICyclicProtocol {

	private final IndexedNeighborGraph fGraph;

	private final int fId;

	private final int fPid;

	private double fEndToEndDelay;

	private double fRawReceiverDelay;

	private EDSimulationEngine fParent;

	private BitSet fHistory;

	private BitSet fMappedHistory;

	private final IPeerSelector fSelector;

	private final ILiveTransformer fTransformer;

	private final IProcess fProcess;

	private State fState = State.WAITING;

	private boolean fInitiallyReachable = false;

	public HFlood(IndexedNeighborGraph graph, IPeerSelector selector,
			IProcess process, ILiveTransformer transformer, int id, int pid) {
		fId = id;
		fPid = pid;
		fGraph = graph;
		fHistory = new BitSet();
		fMappedHistory = new BitSet();
		fSelector = selector;
		fTransformer = transformer;
		fProcess = process;

		clearState();
	}

	public void clearState() {
		fHistory.clear();
		fEndToEndDelay = Double.NEGATIVE_INFINITY;
		fRawReceiverDelay = Double.NEGATIVE_INFINITY;
	}

	@Override
	public void nextCycle(double time, INetwork sim, IProcess process) {

		// Are we done, down, or not reached yet?
		if (fState == State.DONE || !sim.process(fId).isUp()
				|| !fHistory.get(fId)) {
			return;
		}

		// Tries to get a peer.
		int neighborId = selectPeer(sim);

		// No live neighbors at the moment, so we have to wait.
		if (neighborId < 0) {
			fState = State.WAITING;
			return;
		}

		// Otherwise we're active.
		fState = State.ACTIVE;

		// Sends the message and gets the feedback.
		HFlood neighbor = (HFlood) sim.process(neighborId).getProtocol(fPid);
		fHistory.or(neighbor.sendMessage(fHistory, time));

		checkDone();
	}

	public int id() {
		return fId;
	}

	public IndexedNeighborGraph graph() {
		return fGraph;
	}

	private void checkDone() {
		if (fState == State.DONE) {
			return;
		}

		// Checks if all of our neighbors have gotten the message,
		// including us.
		if (!fHistory.get(fId)) {
			return;
		}

		for (int i = 0; i < fGraph.degree(fId); i++) {
			if (!fHistory.get(fGraph.getNeighbor(fId, i))) {
				return;
			}
		}

		fState = State.DONE;
	}

	private int selectPeer(INetwork sim) {
		Triplet<AbstractIDMapper, INetwork, IndexedNeighborGraph> transform = fTransformer
				.live(fGraph, sim);

		if (transform == null) {
			return fSelector.selectPeer(fId, fGraph, fHistory, sim);
		}

		IDMapper mapper = transform.a;
		int selected = fSelector.selectPeer(mapper.map(fId), transform.c,
				remapHistory(mapper, fHistory), transform.b);

		return selected >= 0 ? mapper.reverseMap(selected) : selected;
	}

	private BitSet remapHistory(IDMapper mapper, BitSet history) {
		fMappedHistory.clear();
		for (int i = history.nextSetBit(0); i >= 0; i = history
				.nextSetBit(i + 1)) {
			if (mapper.isMapped(i)) {
				fMappedHistory.set(mapper.map(i));
			}
		}
		return fMappedHistory;
	}

	private BitSet sendMessage(BitSet history, double time) {
		markReached(time);
		fHistory.or(history);
		checkDone();
		return fHistory;
	}

	public void markReached(double time) {
		if (!isReached()) {
			fEndToEndDelay = time;
			fRawReceiverDelay = fProcess.uptime(fParent);
			fHistory.set(fId);
			fState = State.ACTIVE;
		}
	}

	public boolean isReached() {
		return fEndToEndDelay != Double.NEGATIVE_INFINITY;
	}

	public double rawEndToEndDelay() {
		return fEndToEndDelay;
	}

	public double rawReceiverDelay() {
		return fRawReceiverDelay;
	}

	@Override
	public State getState() {
		return fState;
	}

	void partOfConnectedCore(boolean mark) {
		fInitiallyReachable = mark;
	}
	
	public boolean isPartOfConnectedCore() {
		return fInitiallyReachable;
	}


	public SourceListener sourceEventObserver() {
		return new SourceListener(this, fPid);
	}
}
/**
 * This listener does three things:
 * <ol>
 * <li>identifies the initial login of the source node;</li>
 * <li>identifies the initial set of nodes reachable from the source when it
 * initially logs-in;</li>
 * <li>maintains the initial core, removing nodes that go down before being
 * reached by the push protocol.</li>
 * </ol>
 * 
 * It should only be registered for the dissemination source. In case these
 * functions are not desired, simply do not register the listener.
 */
class SourceListener implements ISimulationObserver, INetworkMetric {

	private final HFlood fSource;

	private final int fPid;

	private EDSimulationEngine fEngine;

	private int fReachable = -1;
	
	private double [] fSnapshot;
	
	public SourceListener(HFlood source, int pid) {
		fSource = source;
		fPid = pid;
	}

	@Override
	public void eventPerformed(INetwork network, double time,
			Schedulable schedulable) {

		IProcess process = (IProcess) schedulable;

		if (sourceReached()) {
			// If process went down, it might have to be removed from
			// the initial core.
			if (!process.isUp()) {
				removeFromCore(network, time, process);
			}
		}

		// This is the first log-in event of the source.
		else if (process.id() == fSource.id() && process.isUp()) {
			fSource.markReached(time);
			identifyCore(network);
			// We're no longer binding.
			fEngine.unbound(this);
		}
	}

	/**
	 * Removes from the initial search any node that went down before being
	 * reached by the push protocol.
	 */
	private void removeFromCore(INetwork network, double time, IProcess process) {
		HFlood protocol = (HFlood) process.getProtocol(fPid);
		if (protocol.isPartOfConnectedCore() && protocol.isReached()) {
			protocol.partOfConnectedCore(false);
			fReachable--;
		}
	}

	private boolean sourceReached() {
		return fReachable >= 0;
	}

	/**
	 * Identifies the initial connected core of nodes, marking those that are
	 * reachable from a depth-first search started from the source.
	 */
	private void identifyCore(final INetwork network) {
		IndexedNeighborGraph graph = fSource.graph();
		boolean[] reachable = new boolean[graph.size()];
		GraphAlgorithms.dfs(graph, fSource.id(), reachable, new IEdgeFilter() {
			@Override
			public boolean isForbidden(int i, int j) {
				return !network.process(j).isUp();
			}
		});

		fReachable = 0;
		for (int i = 0; i < reachable.length; i++) {
			if (reachable[i]) {
				((HFlood) network.process(i).getProtocol(fPid))
						.partOfConnectedCore(true);
				fReachable++;
			}
		}
	}

	@Override
	public void simulationStarted(EDSimulationEngine parent) {
		fEngine = parent;
		eventPerformed(parent, parent.currentTime(), parent.process(fSource.id()));
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public boolean isBinding() {
		return true;
	}

	@Override
	public Object id() {
		return "rd";
	}

	@Override
	public double getMetric(int i) {
		if (fSnapshot == null) {
			throw new IllegalStateException("Source not yet reached!");
		}
		HFlood protocol = (HFlood) fEngine.process(i).getProtocol(fPid);
		return protocol.rawReceiverDelay() - fSnapshot[i];
	}

}
