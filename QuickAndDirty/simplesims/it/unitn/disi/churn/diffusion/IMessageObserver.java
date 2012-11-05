package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.IClockData;
import it.unitn.disi.simulator.core.IProcess;

public interface IMessageObserver {

	public void messageReceived(IProcess process, HFloodMMsg message,
			IClockData clock, boolean duplicate);

}
