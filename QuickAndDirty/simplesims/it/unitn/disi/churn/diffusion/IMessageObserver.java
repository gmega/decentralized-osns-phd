package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.IClockData;

public interface IMessageObserver {

	public void messageReceived(HFloodMMsg message, IClockData clock);
	
}
