package it.unitn.disi.churn.connectivity;

import java.util.Iterator;
import java.util.LinkedList;

import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.measure.IMetricAccumulator;
import it.unitn.disi.simulator.measure.INodeMetric;

@Binding
public class MultiTCE implements IEventObserver {

	private final LinkedList<SimpleTCE> fActive = new LinkedList<SimpleTCE>();

	private final IndexedNeighborGraph fGraph;

	private final int fSource;

	private final int fTotal;

	private final IMetricAccumulator<Double> fAccumulator;

	private int fComplete;

	public MultiTCE(IndexedNeighborGraph graph, int source, int samples) {
		this(graph, source, samples, null);
	}

	public MultiTCE(IndexedNeighborGraph graph, int source, int samples,
			IMetricAccumulator<Double> accumulator) {
		fTotal = samples;
		fGraph = graph;
		fSource = source;
		fAccumulator = accumulator;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {
		if (isSourceLogin(schedulable) && (fActive.size() + fComplete < fTotal)) {
			fActive.add(new SimpleTCE(fGraph, fSource));
		}

		Iterator<SimpleTCE> it = fActive.iterator();
		while (it.hasNext()) {
			SimpleTCE tce = it.next();
			tce.eventPerformed(engine, schedulable, nextShift);
			if (tce.isDone()) {
				it.remove();
				fComplete++;
				add(tce);
			}
		}

		if (isDone()) {
			engine.unbound(this);
		}
	}

	private void add(final SimpleTCE tce) {
		fAccumulator.add(new INodeMetric<Double>() {

			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public Double getMetric(int i) {
				return tce.reachTime(i);
			}
		});
	}

	private boolean isSourceLogin(Schedulable schedulable) {
		IProcess process = (IProcess) schedulable;
		return process.id() == fSource;
	}

	@Override
	public boolean isDone() {
		return fComplete >= fTotal;
	}

}
