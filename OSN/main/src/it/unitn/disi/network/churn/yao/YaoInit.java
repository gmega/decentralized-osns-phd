package it.unitn.disi.network.churn.yao;

import it.unitn.disi.simulator.random.IDistribution;
import it.unitn.disi.simulator.yao.YaoPresets;
import it.unitn.disi.simulator.yao.YaoPresets.IAverageGenerator;
import it.unitn.disi.simulator.yao.YaoPresets.IDistributionGenerator;
import it.unitn.disi.utils.logging.StructuredLog;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.tabular.ITableWriter;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.dynamics.NodeInitializer;

/**
 * Initializes the Yao model according to the <a
 * href="http://dx.doi.org/10.1109/ICNP.2006.320196">original paper</a>. <BR>
 * <BR>
 * The preset configurations from the paper are in class {@link YaoPresets}.
 */
@StructuredLog(key = "YaoInit", fields = { "id", "li", "di", "eli", "edi", "ai" })
@AutoConfig
public class YaoInit implements Control, NodeInitializer {

	// ------------------------------------------------------------------------

	private int fYaoChurnId;

	private ITableWriter fLog;

	private final IDistributionGenerator fMode;

	private final IAverageGenerator fAverages;

	public YaoInit(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute IResolver resolver,
			@Attribute("protocol") int yaoChurnId,
			@Attribute("mode") String mode,
			@Attribute(value = "generator", defaultValue = "YAO") String generator,
			@Attribute("TabularLogManager") TabularLogManager logManager) {
		fYaoChurnId = yaoChurnId;
		fLog = logManager.get(YaoInit.class);
		fMode = mode(resolver, prefix, mode);
		fAverages = generator(resolver, prefix, generator);
	}

	@Override
	public boolean execute() {
		// Assigns different distributions to each node.
		for (int i = 0; i < Network.size(); i++) {
			initialize(Network.get(i));
		}
		return false;
	}

	@Override
	public void initialize(Node current) {
		YaoOnOffChurn churn = (YaoOnOffChurn) current.getProtocol(fYaoChurnId);
		double li = fAverages.nextLI();
		double di = fAverages.nextDI();
		IDistribution uptime = fMode.uptimeDistribution(li);
		IDistribution downtime = fMode.downtimeDistribution(di);
		churn.init(uptime, downtime, current);
		printParameters(current, li, di, uptime.expectation(),
				downtime.expectation());
	}

	private void printParameters(Node node, double li, double di, double eli,
			double edi) {
		fLog.set("id", Long.toString(node.getID()));
		fLog.set("li", Double.toString(li));
		fLog.set("di", Double.toString(di));
		// Expectation of assigned uptime distribution.
		fLog.set("eli", Double.toString(eli));
		// Expectation of assigned downtime distribution.
		fLog.set("edi", Double.toString(edi));
		// Availability.
		double availability = li / (li + di);
		fLog.set("ai", Double.toString(availability));
		fLog.emmitRow();
	}

	@SuppressWarnings("unchecked")
	private IDistributionGenerator mode(IResolver resolver, String prefix,
			String modeId) {
		IDistributionGenerator generator = YaoPresets.mode(modeId,
				CommonState.r);
		if (generator != null) {
			return generator;
		}

		// Not one of the pre-set modes, interpret as class name.
		return (IDistributionGenerator) ObjectCreator.createInstance(
				Configuration.getClass(prefix + ".mode"), prefix, resolver);
	}

	@SuppressWarnings("unchecked")
	private IAverageGenerator generator(IResolver resolver, String prefix,
			String modeId) {

		IAverageGenerator generator = YaoPresets.averageGenerator(modeId,
				CommonState.r);
		if (generator != null) {
			return generator;
		}

		// Not one of the pre-set modes, interpret as class name.
		return (IAverageGenerator) ObjectCreator
				.createInstance(Configuration.getClass(prefix + ".generator"),
						prefix, resolver);
	}

}
