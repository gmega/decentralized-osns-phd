package it.unitn.disi.newscasting.experiments;

import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import it.unitn.disi.newscasting.IContentExchangeStrategy;
import it.unitn.disi.newscasting.Tweet;
import it.unitn.disi.newscasting.internal.ICoreInterface;
import it.unitn.disi.newscasting.internal.IEventObserver;
import it.unitn.disi.utils.IReference;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.peersim.IScheduler;
import it.unitn.disi.utils.peersim.PeersimUtils;
import it.unitn.disi.utils.peersim.ProtocolReference;
import it.unitn.disi.utils.peersim.SNNode;

/**
 * Monitoring protocol which halts dissemination after a certain time amount has
 * passed. Only suitable within the unit experiment framework.
 * 
 * @author giuliano
 */
@AutoConfig
@StructuredLog(key="TC", fields={"root", "id", "exptime", "uptime"})
public class TimeoutController implements IEventObserver, EDProtocol<Object> {

	private static final Object SHUTDOWN_EVT = new Object();

	private final IReference<IContentExchangeStrategy> fXchgRef;

	private final IReference<IScheduler<Object>> fSchedulerRef;

	private final int fTimeReserve;

	private final int fSelfPid;
	
	private final TableWriter fLog;

	public TimeoutController(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute("TabularLogManager") TabularLogManager manager,
			@Attribute("timeout") int timeout,
			@Attribute("application") int appid,
			@Attribute("scheduler") int schedulerId) {
		if (timeout == 0) {
			throw new IllegalArgumentException(
					"Timeout must be greater than zero.");
		}
		fTimeReserve = timeout;
		fXchgRef = new ExchangeStrategyRef(appid, prefix + ".strategy_id");
		fSchedulerRef = new ProtocolReference<IScheduler<Object>>(schedulerId);
		fSelfPid = PeersimUtils.selfPid(prefix);
		fLog = manager.get(TimeoutController.class);
	}

	@Override
	public void tweeted(Tweet tweet) {
		startClock(tweet.profile());
	}

	@Override
	public void eventDelivered(SNNode sender, SNNode receiver, Tweet tweet,
			boolean duplicate) {
		if (!duplicate) {
			startClock(receiver);
		}
	}

	@Override
	public void processEvent(Node node, int pid, Object event) {
		if (event == SHUTDOWN_EVT) {
			shutdownNode(node);
		}
	}

	@Override
	public Object clone() {
		return this;
	}

	protected void shutdownNode(Node node) {
		IContentExchangeStrategy strategy = fXchgRef.get(node);
		strategy.clear(node);
		
		//FIXME Argh static, implicit component wiring.
		DisseminationExperimentGovernor gov = DisseminationExperimentGovernor.singletonInstance();
		fLog.set("root", gov.currentNode().getID());
		fLog.set("id", node.getID());
		fLog.set("exptime", gov.experimentTime());
		fLog.set("uptime", ((SNNode)node).uptime());
		fLog.emmitRow();
	}

	private void startClock(Node node) {
		IScheduler<Object> scheduler = fSchedulerRef.get(node);
		scheduler.schedule(fTimeReserve, fSelfPid, node, SHUTDOWN_EVT);
	}

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
			ICoreInterface intf = (ICoreInterface) owner.getProtocol(fAppId);
			return intf.getStrategy(fClass);
		}

	}
}
