package it.unitn.disi.simulator.protocol;

import it.unitn.disi.simulator.core.Binding;
import it.unitn.disi.simulator.core.INetwork;
import it.unitn.disi.simulator.core.IProcess;
import it.unitn.disi.simulator.core.IEventObserver;
import it.unitn.disi.simulator.core.Schedulable;
import it.unitn.disi.simulator.core.ISimulationEngine;
import it.unitn.disi.simulator.protocol.ICyclicProtocol.State;

@Binding
public class CyclicProtocolRunner<K extends ICyclicProtocol> implements
		IEventObserver {

	private static final long serialVersionUID = -4612665670548461258L;

	protected final int fPid;

	private boolean fDone;

	public CyclicProtocolRunner(int pid) {
		fPid = pid;
	}

	@Override
	public void eventPerformed(ISimulationEngine engine, Schedulable schedulable,
			double nextShift) {

		INetwork network = engine.network();

		int done = 0;
		for (int i = 0; i < network.size(); i++) {
			IProcess process = network.process(i);
			ICyclicProtocol protocol = (ICyclicProtocol) process
					.getProtocol(fPid);
			State protocolState = protocol.getState();
			
			if (protocolState != State.DONE) {
				protocol.nextCycle(engine, process);
			} 
			
			if (hasReachedEndState(engine, protocol)) {			
				done++;
			}
		}

		if (done == network.size()) {
			fDone = true;
			engine.unbound(this);
		}
	}

	@Override
	public boolean isDone() {
		return fDone;
	}
	
	protected boolean hasReachedEndState(ISimulationEngine engine, ICyclicProtocol protocol) {
		return protocol.getState() == State.DONE;
	}

}
