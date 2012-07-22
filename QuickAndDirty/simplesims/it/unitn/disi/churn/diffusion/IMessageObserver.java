package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.IClockData;

public interface IMessageObserver {

	public void messageReceived(Message message, IClockData clock);
	
}
