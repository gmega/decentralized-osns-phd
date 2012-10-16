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
import it.unitn.disi.utils.logging.IProgressTracker;
import it.unitn.disi.utils.logging.Progress;
import it.unitn.disi.utils.logging.ProgressTracker;

@Binding
public class MultiTCE implements IEventObserver {

	private final LinkedList<SimpleTCE> fActive = new LinkedList<SimpleTCE>();

	private final IndexedNeighborGraph fGraph;

	private final int fSource;

	private final int fTotal;

	private final IMetricAccumulator<Double> fEd;
	
	private final IMetricAccumulator<Double> fRd;

	private final IProgressTracker fTracker;

	private int fComplete;

	private int fMaxActive;

	public MultiTCE(IndexedNeighborGraph graph, int source, int samples) {
		this(graph, source, samples, null, null, IProgressTracker.NULL_TRACKER);
	}

	public MultiTCE(IndexedNeighborGraph graph, int source, int samples,
			IMetricAccumulator<Double> ed, IMetricAccumulator<Double> rd,
			IProgressTracker tracker) {
		fTotal = samples;
		fGraph = graph;
		fSource = source;
		fEd = ed;
		fRd = rd;
		fTracker = tracker;
		fTracker.startTask();
	}

	@Override
	public void eventPerformed(ISimulationEngine engine,
			Schedulable schedulable, double nextShift) {
		if (isSourceLogin(schedulable) && (fActive.size() + fComplete < fTotal)) {
			fActive.add(new SimpleTCE(fGraph, fSource));
		}

		fMaxActive = Math.max(fActive.size(), fMaxActive);
		Iterator<SimpleTCE> it = fActive.iterator();
		while (it.hasNext()) {
			SimpleTCE tce = it.next();
			tce.eventPerformed(engine, schedulable, nextShift);
			if (tce.isDone()) {
				it.remove();
				fComplete++;
				fTracker.tick();
				if (fComplete % 1000 == 0) {
					System.err.println("Active: " + fActive.size() + ", Max: "
							+ fMaxActive);
				}
				add(tce);
			}
		}

		if (isDone()) {
			engine.stop();
		}
	}

	private void add(final SimpleTCE tce) {
		fEd.add(new INodeMetric<Double>() {

			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public Double getMetric(int i) {
				return tce.reachTime(i);
			}
		});
		
		fRd.add(new INodeMetric<Double>() {

			@Override
			public Object id() {
				return "rd";
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
