package it.unitn.disi.churn.diffusion;

import it.unitn.disi.churn.config.ExperimentReader.Experiment;
import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.IProcess;

import java.util.Random;
import java.util.concurrent.Callable;

public abstract class DiffusionSimulationTask implements
		Callable<DiffusionSimulationTask> {

	public static final int HFLOOD_PID = 0;
	
	protected HFlood [] fProtocols;

	protected IProcess[] processes(Experiment experiment, int source,
			IndexedNeighborGraph graph, Random r, HFlood[] protos) {

		IProcess[] rp = new IProcess[graph.size()];

		for (int i = 0; i < rp.length; i++) {
			Object[] pArray = new Object[] { protos[i] };
			rp[i] = create(i, experiment, pArray);
		}

		return rp;
	}

	protected HFlood[] protocols(IndexedNeighborGraph graph, Random r,
			String peerSelector, ILiveTransformer transformer) {
		fProtocols = new HFlood[graph.size()];

		for (int i = 0; i < graph.size(); i++) {
			fProtocols[i] = new HFlood(graph, peerSelector(r, peerSelector),
					transformer, i, HFLOOD_PID);
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

	protected abstract IProcess create(int i, Experiment exp, Object[] pArray);
}
