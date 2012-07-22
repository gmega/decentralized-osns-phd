package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.INetwork;

public class PIDReference<K> implements IProtocolReference<K> {

	private final int fPid;

	public PIDReference(int pid) {
		fPid = pid;
	}

	@SuppressWarnings("unchecked")
	@Override
	public K get(K caller, INetwork network, int id) {
		return (K) network.process(id).getProtocol(fPid);
	}

}
