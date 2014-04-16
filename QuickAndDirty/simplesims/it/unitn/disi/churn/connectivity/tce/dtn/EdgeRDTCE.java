package it.unitn.disi.churn.connectivity.tce.dtn;

import java.util.BitSet;

import it.unitn.disi.churn.connectivity.tce.SimpleRDTCE;
import it.unitn.disi.graph.IGraphVisitor;
import it.unitn.disi.graph.lightweight.LightweightStaticGraphEID;
import it.unitn.disi.simulator.core.EngineBuilder;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.IProcess.State;
import it.unitn.disi.simulator.protocol.FixedProcess;
import it.unitn.disi.simulator.random.Exponential;
import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.random.MTUnitUniformDistribution;
import it.unitn.disi.simulator.random.MersenneTwister;

/**
 * Temporal connectivity experiment for the "independent edge delays" case;
 * i.e., when edges go up and down instead of processes. This approximates
 * better how DTNs are usually modeled.
 * 
 * @author giuliano
 */
public class EdgeRDTCE extends SimpleRDTCE {

	private static final long serialVersionUID = 1L;

	private final int fType;

	/**
	 * Bitmap containing the up/down state of each edge in the graph.
	 */
	private final BitSet fStateMap;

	/**
	 * Bitmap marking which edges are expired, and which are not.
	 */
	private final BitSet fExpired;

	/**
	 * Parameters for the 
	 */
	private final double[] fRates;

	private final IDistribution fU = new MTUnitUniformDistribution(
			new MersenneTwister());

	public EdgeRDTCE(int source, int type, LightweightStaticGraphEID graph,
			final EngineBuilder builder, double[] rates) {

		super(graph, source);

		if (graph.directed()) {
			throw new IllegalArgumentException("Input graph can't be directed.");
		}

		fType = type;
		fRates = rates;
		fStateMap = new BitSet(graph.edgeCount());
		fExpired = new BitSet(graph.edgeCount());

		builder.addProcess(fixedProcesses(graph.size()));

		graph.visit(new IGraphVisitor() {
			private int fIndex;

			@Override
			public void visitVertex(int i) {
				fIndex = 0;
			}

			@Override
			public void visitEdge(int i, int j) {
				builder.preschedule(new EdgeSchedulable(i, fIndex));
				fIndex++;
			}
		});

		builder.addObserver(this, fType, true, true);

		/* Will "paint" the source as soon as burnin finishes. */
		builder.addBurninAction(new IEventObserver() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isDone() {
				return false;
			}

			@Override
			public void eventPerformed(ISimulationEngine engine,
					Schedulable schedulable, double nextShift) {
				markSource(engine);
			}
		});
	}

	private IProcess[] fixedProcesses(int graphSize) {
		IProcess[] processes = new IProcess[graphSize];
		for (int i = 0; i < processes.length; i++) {
			processes[i] = new FixedProcess(i, State.up, false);
		}

		return processes;
	}

	@Override
	public boolean canIncreaseConnectivity(Schedulable schedulable) {
		EdgeSchedulable edge = (EdgeSchedulable) schedulable;
		// As with processes, only edges that come up can increase connectivity.
		// Edges going down can at most reduce it.
		return fStateMap.get(graph().rawEdgeId(edge.i, edge.offset));
	}

	@Override
	protected boolean canFlow(int i, int j, int index, INetwork network) {
		return fStateMap.get(graph().edgeId(i, j, index));
	}

	@Override
	protected void mark(int source, int node, ISimulationEngine engine) {
		super.mark(source, node, engine);
		// Once edges are traversed, they are removed from the simulation.
		fExpired.set(graph().indexOf(source, node));
	}

	private LightweightStaticGraphEID graph() {
		return (LightweightStaticGraphEID) fGraph;
	}

	private void flipState(EdgeSchedulable edge) {
		fStateMap.flip(graph().rawEdgeId(edge.i, edge.offset));
	}

	private boolean expired(EdgeSchedulable edge) {
		return fExpired.get(graph().rawEdgeId(edge.i, edge.offset));
	}

	private double sample(EdgeSchedulable edge) {
		return Exponential.sample(
				fRates[graph().rawEdgeId(edge.i, edge.offset)], fU);
	}

	/**
	 * Simple {@link Schedulable} for graph edges, which may be active/non
	 * active. The main design goal is to keep memory footprint small, as tens
	 * of millions of these have to be scheduled even for small graphs.
	 * 
	 * @author giuliano
	 */
	public class EdgeSchedulable extends Schedulable {

		private static final long serialVersionUID = 1L;

		public final int i;

		public final int offset;

		private double fTime;

		private EdgeSchedulable(int i, int offset) {
			this.i = checkPositive(i);
			this.offset = checkPositive(offset);
		}

		private int checkPositive(int i) {
			if (i < 0) {
				throw new IllegalArgumentException("Edge id can't be negative.");
			}

			return i;
		}

		@Override
		public boolean isExpired() {
			return expired(this);
		}

		@Override
		public void scheduled(ISimulationEngine state) {
			fTime += sample(this);
			flipState(this);
		}

		@Override
		public double time() {
			return fTime;
		}

		@Override
		public int type() {
			return fType;
		}

	}

}
