package it.unitn.disi.churn.diffusion;

import it.unitn.disi.simulator.core.IClockData;

public interface IMessageObserver {

	public void messageReceived(int sender, int receiver, HFloodMMsg message,
			IClockData clock, int flags);

}
