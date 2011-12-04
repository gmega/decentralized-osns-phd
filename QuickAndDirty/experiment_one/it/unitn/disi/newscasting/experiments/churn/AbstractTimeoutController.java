package it.unitn.disi.newscasting.experiments.churn;

import it.unitn.disi.epidemics.IContentExchangeStrategy;
import it.unitn.disi.epidemics.IContentExchangeStrategy.ActivityStatus;
import it.unitn.disi.epidemics.IProtocolSet;
import it.unitn.disi.unitsim.GovernorBase;
import it.unitn.disi.utils.MiscUtils;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.SNNode;
import it.unitn.disi.utils.tabular.IReference;
import it.unitn.disi.utils.tabular.ITableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;

/**
 * Monitoring protocol which halts dissemination after a certain time amount has
 * passed. Only suitable within the unit experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
@StructuredLog(key="TC", fields={"root", "id", "exptime", "uptime", "disstime"})
public abstract class AbstractTimeoutController implements EDProtocol<Object> {

	protected static final Object SHUTDOWN_EVT = new Object();

	private final IReference<IContentExchangeStrategy> fXchgRef;

	protected final int fTimeReserve;

	protected final int fSelfPid;
	
	private final ITableWriter fLog;
	
	private final GovernorBase fGovernor;

	public AbstractTimeoutController(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("timeout") int timeout,
			@Attribute("application") int appid,
			@Attribute("CDGovernor") GovernorBase governor) {
		if (timeout == 0) {
			throw new IllegalArgumentException(
					"Timeout must be greater than zero.");
		}
		fTimeReserve = timeout;
		fGovernor = governor;
		fXchgRef = new ExchangeStrategyRef(appid, prefix + ".strategy_id");
		fSelfPid = PeersimUtils.selfPid(prefix);
		fLog = manager.get(AbstractTimeoutController.class);
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if (event == SHUTDOWN_EVT) {
			shutdownNode(node);
		}
	}

	@Override
	public Object clone() {
		try {
			AbstractTimeoutController cloned = (AbstractTimeoutController) super.clone();
			return cloned;
		} catch (CloneNotSupportedException ex) {
			throw MiscUtils.nestRuntimeException(ex);
		}
	}

	private void shutdownNode(Node node) {
		IContentExchangeStrategy strategy = fXchgRef.get(node);
		if (strategy.status() == ActivityStatus.ACTIVE) {
			strategy.clear(node);

			fLog.set("root", fGovernor.currentExperiment().getId());
			fLog.set("id", node.getID());
			fLog.set("exptime", fGovernor.experimentTime());
			fLog.set("uptime", ((SNNode) node).uptime(true));
			fLog.set("disstime", timeoutTime());
			fLog.emmitRow();
		}
	}

	public abstract void startTimeout(Node node);
	
	protected abstract long timeoutTime();

	protected long getTime() {
		return CommonState.getTime();
	}

	private static class ExchangeStrategyRef implements
			IReference<IContentExchangeStrategy> {

		private final Class<? extends IContentExchangeStrategy> fClass;

		private final int fAppId;

		@SuppressWarnings("unchecked")
		public ExchangeStrategyRef(int appid, String klass) {
			fClass = Configuration.getClass(klass);
			fAppId = appid;
		}

		@Override
		public IContentExchangeStrategy get(Node owner) {
			IProtocolSet intf = (IProtocolSet) owner.getProtocol(fAppId);
			return intf.getStrategy(fClass);
		}

	}
}
