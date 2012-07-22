package it.unitn.disi.churn.diffusion.config;

import it.unitn.disi.churn.diffusion.BiasedCentralitySelector;
import it.unitn.disi.churn.diffusion.HFloodSM;
import it.unitn.disi.churn.diffusion.IPeerSelector;
import it.unitn.disi.churn.diffusion.IProtocolReference;
import it.unitn.disi.churn.diffusion.PIDReference;
import it.unitn.disi.churn.diffusion.RandomSelector;
import it.unitn.disi.churn.diffusion.graph.ILiveTransformer;
import it.unitn.disi.graph.IndexedNeighborGraph;
import it.unitn.disi.simulator.core.IProcess;

import java.util.Random;

public abstract class DiffusionSimulationBuilder {

	public static final int HFLOOD_PID = 0;

	protected HFloodSM[] fProtocols;

	protected HFloodSM[] protocols(IndexedNeighborGraph graph, Random r,
			String peerSelector, ILiveTransformer transformer,
			IProcess[] processes) {
		fProtocols = new HFloodSM[graph.size()];

		IProtocolReference<HFloodSM> ref = new PIDReference<HFloodSM>(
				HFLOOD_PID);

		for (int i = 0; i < graph.size(); i++) {
			fProtocols[i] = new HFloodSM(graph, peerSelector(r, peerSelector),
					processes[i], transformer, ref);
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

}
