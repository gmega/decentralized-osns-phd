package it.unitn.disi.churn.diffusion.config;

import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.HFlood;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.RandomSelector;
import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.measure.INodeMetric;

import java.util.List;
import java.util.Random;

public abstract class DiffusionSimulationBuilder {

	public static final int HFLOOD_PID = 0;

	protected HFlood[] fProtocols;

	protected HFlood[] protocols(IndexedNeighborGraph graph, Random r,
			String peerSelector, ILiveTransformer transformer,
			IProcess[] processes) {
		fProtocols = new HFlood[graph.size()];

		for (int i = 0; i < graph.size(); i++) {
			fProtocols[i] = new HFlood(graph, peerSelector(r, peerSelector),
					processes[i], transformer, i, HFLOOD_PID);
			processes[i].addProtocol(fProtocols[i]);
		}

		return fProtocols;
	}

	protected IPeerSelector peerSelector(Random r, String selector) {
		switch (selector.charAt(0)) {
		case 'a':
			return new BiasedCentralitySelector(r, true);
		case 'r':
			return new RandomSelector(r);
		case 'c':
			return new BiasedCentralitySelector(r, false);
		default:
			throw new UnsupportedOperationException();
		}
	}

	protected void addEDMetric(List<INodeMetric<?>> metrics,
			final HFlood[] protocols, final int source) {
		metrics.add(new INodeMetric<Double>() {

			@Override
			public Object id() {
				return "ed";
			}

			@Override
			public Double getMetric(int i) {
				return protocols[i].rawEndToEndDelay()
						- protocols[source].rawEndToEndDelay();
			}
		});
	}
}
