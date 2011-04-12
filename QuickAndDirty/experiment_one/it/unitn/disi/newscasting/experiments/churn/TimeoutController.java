package it.unitn.disi.newscasting.experiments.churn;

import it.unitn.disi.utils.logging.TabularLogManager;
import peersim.cdsim.CDProtocol;
import peersim.config.Attribute;
import peersim.core.Node;

public class TimeoutController extends AbstractTimeoutController implements CDProtocol {
	
	private int fTimeRemaining = -1;
	
	private int fTotalTime = -1;

	public TimeoutController(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("timeout") int timeout,
			@Attribute("application") int appid) {
		super(prefix, manager, timeout, appid);
	}
	
	@Override
	public void startTimeout(Node node) {
		fTimeRemaining = fTimeReserve;
		if (fTotalTime == -1) {
			fTotalTime = 0;
		}
	}

	@Override
	public void nextCycle(Node node, int protocolID) {
		fTimeRemaining--;
		if (fTotalTime > 0) {
			fTotalTime++;
		}
		if (fTimeRemaining == 0) {
			processEvent(node, fSelfPid, SHUTDOWN_EVT);
		}
	}
	
	@Override
	protected long timeoutTime() {
		return fTotalTime;
	}

}
