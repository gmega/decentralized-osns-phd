package it.unitn.disi.churn.connectivity.tce;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.ISimulationEngine;

/**
 * Binding extension to {@link SimpleTCE} -- i.e., simulation is not allowed to
 * stop until {@link SimpleTCE#isDone()} becomes true.
 * 
 * @author giuliano
 */
@Binding
public class BindingSimpleTCE extends SimpleTCE {

	private static final long serialVersionUID = 1L;

	private boolean fBinding;

	public BindingSimpleTCE(IndexedNeighborGraph graph, int source) {
		this(graph, source, true);
	}

	public BindingSimpleTCE(IndexedNeighborGraph graph, int source,
			boolean binding) {
		super(graph, source);
		fBinding = binding;
	}

	@Override
	protected void done(ISimulationEngine engine) {
		super.done(engine);
		if (fBinding) {
			engine.unbound(this);
		}
	}

}
