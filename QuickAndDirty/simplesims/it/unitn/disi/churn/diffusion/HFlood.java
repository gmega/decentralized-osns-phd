package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.graph.analysis.GraphAlgorithms;
import it.unitn.disi.graph.analysis.GraphAlgorithms.IEdgeFilter;
import it.unitn.disi.simulator.ICyclicProtocol;
import it.unitn.disi.simulator.IEventObserver;
import it.unitn.disi.simulator.INetwork;
import it.unitn.disi.simulator.IProcess;
import it.unitn.disi.simulator.Schedulable;
import it.unitn.disi.simulator.SimpleEDSim;
import it.unitn.disi.utils.AbstractIDMapper;
import it.unitn.disi.utils.IDMapper;
import it.unitn.disi.utils.collections.Triplet;

import java.util.BitSet;

public class HFlood implements ICyclicProtocol {

	private final IndexedNeighborGraph fGraph;

	private final int fId;

	private final int fPid;

	private double fReachTime;

	private BitSet fHistory;

	private BitSet fMappedHistory;

	private final IPeerSelector fSelector;

	private final ILiveTransformer fTransformer;

	private State fState = State.WAITING;

	private boolean fInitiallyReachable = false;

	public HFlood(IndexedNeighborGraph graph, IPeerSelector selector,
			ILiveTransformer transformer, int id, int pid) {
		fId = id;
		fPid = pid;
		fGraph = graph;
		fHistory = new BitSet();
		fMappedHistory = new BitSet();
		fSelector = selector;
		fTransformer = transformer;

		clearState();
	}

	public void clearState() {
		fHistory.clear();
		fReachTime = Double.NEGATIVE_INFINITY;
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
			fReachTime = time;
			fHistory.set(fId);
			fState = State.ACTIVE;
		}
	}

	public boolean isReached() {
		return fReachTime != Double.NEGATIVE_INFINITY;
	}

	public double latency() {
		return fReachTime;
	}

	@Override
	public State getState() {
		return fState;
	}

	private void partOfConnectedCore(boolean mark) {
		fInitiallyReachable = mark;
	}

	public boolean isPartOfConnectedCore() {
		return fInitiallyReachable;
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
	 * 
	 * @return an {@link IEventObserver} which will mark the enclosing
	 *         {@link HFlood} instance as "reached" as soon as the
	 *         {@link IProcess} with the sharing id logs in for the first time.
	 */
	public IEventObserver sourceEventObserver() {
		return new IEventObserver() {

			private SimpleEDSim fParent;

			private int fReachable = -1;

			@Override
			public void stateShifted(INetwork network, double time,
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
				else if (process.id() == fId && process.isUp()){
					markReached(time);
					identifyCore(network);
					// We're no longer binding.
					fParent.done(this);
				}
			}

			/**
			 * Removes from the initial search any node that went down before
			 * being reached by the push protocol.
			 */
			private void removeFromCore(INetwork network, double time,
					IProcess process) {
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
			 * Identifies the initial connected core of nodes, marking those
			 * that are reachable from a depth-first search started from the
			 * source.
			 */
			private void identifyCore(final INetwork network) {
				boolean[] reachable = new boolean[fGraph.size()];
				GraphAlgorithms.dfs(fGraph, fId, reachable, new IEdgeFilter() {
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
			public void simulationStarted(SimpleEDSim parent) {
				fParent = parent;
				stateShifted(parent, parent.currentTime(), parent.process(fId));
			}

			@Override
			public boolean isDone() {
				return fHistory.get(fId);
			}

			@Override
			public boolean isBinding() {
				return true;
			}
		};
	}
}
