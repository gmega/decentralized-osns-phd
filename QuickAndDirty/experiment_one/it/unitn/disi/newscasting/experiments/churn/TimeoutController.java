package it.unitn.disi.newscasting.experiments.churn;

import it.unitn.disi.unitsim.GovernorBase;
import it.unitn.disi.utils.logging.TabularLogManager;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.core.Node;
import peersim.core.Protocol;

@AutoConfig
public class TimeoutController extends AbstractTimeoutController implements Protocol {

	private int fTimeRemaining = -1;

	private int fTotalTime = -1;

	public TimeoutController(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("timeout") int timeout,
			@Attribute("application") int appid,
			@Attribute("CDGovernor") GovernorBase governor) {
		super(prefix, manager, timeout, appid, governor);
	}

	public void reset() {
		fTimeRemaining = -1;
		fTotalTime = -1;
	}

	@Override
	public void startTimeout(Node node) {
		fTimeRemaining = fTimeReserve;
		if (fTotalTime == -1) {
			fTotalTime = 0;
		}
	}

	public void tick(Node node) {
		if (fTotalTime >= 0) {
			doTick(node);
		}
	}

	private void doTick(Node node) {
		fTotalTime++;
		fTimeRemaining--;
		if (fTimeRemaining == 0) {
			processEvent(node, fSelfPid, SHUTDOWN_EVT);
		}
	}

	@Override
	protected long timeoutTime() {
		return fTotalTime;
	}

	@Override
	public Object clone() {
		return (TimeoutController) super.clone();
	}
}
