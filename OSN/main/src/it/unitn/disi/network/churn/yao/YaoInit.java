package it.unitn.disi.network.churn.yao;

import it.unitn.disi.random.Exponential;
import it.unitn.disi.random.IDistribution;
import it.unitn.disi.random.ShiftedPareto;
import it.unitn.disi.utils.TableWriter;
import it.unitn.disi.utils.logging.TabularLogManager;
import it.unitn.disi.utils.logging.StructuredLog;
import peersim.config.Attribute;
import peersim.config.AutoConfig;
import peersim.config.Configuration;
import peersim.config.IResolver;
import peersim.config.ObjectCreator;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Initializes the Yao model according to the <a
 * href="http://dx.doi.org/10.1109/ICNP.2006.320196">original paper</a>.
 */
@StructuredLog(key = "YaoInit", fields = { "id", "index", "li", "di", "eli",
		"edi", "ai" })
@AutoConfig
public class YaoInit implements Control {

	// ------------------------------------------------------------------------
	// Configuration machinery.
	// ------------------------------------------------------------------------

	interface IMode {
		IDistribution uptimeDistribution(double li);

		IDistribution downtimeDistribution(double di);

		String id();
	}

	// -- Heavy tailed
	private static final IMode HEAVY_TAILED = new DualPareto(3.0, 3.0, 2.0,
			2.0, "H", CommonState.r);

	// -- Very Heavy tailed
	private static final IMode VERY_HEAVY_TAILED = new DualPareto(1.5, 1.5,
			2.0, 2.0, "VH", CommonState.r);

	// -- Exponential System
	private static final IMode EXPONENTIAL_SYSTEM = new IMode() {

		@Override
		public IDistribution uptimeDistribution(double li) {
			return new Exponential(1.0 / li, CommonState.r);
		}

		@Override
		public IDistribution downtimeDistribution(double di) {
			return new ShiftedPareto(3.0, 2.0 * di, CommonState.r);
		}

		@Override
		public String id() {
			return "E";
		}

	};

	private static final IMode[] modes = new IMode[] { HEAVY_TAILED,
			VERY_HEAVY_TAILED, EXPONENTIAL_SYSTEM };

	/**
	 * Alpha parameters for the shifted Pareto distributions used to generate
	 * uptime and downtime averages.
	 */
	private static final double ALPHA = 3.0;

	private static final double BETA_UPTIME = 1.0;

	private static final double BETA_DOWNTIME = 2.0;

	private int fYaoChurnId;

	private TableWriter fLog;

	private final IMode fMode;

	public YaoInit(@Attribute(Attribute.PREFIX) String prefix,
			@Attribute IResolver resolver,
			@Attribute("protocol") int yaoChurnId,
			@Attribute("mode") String mode,
			@Attribute("TabularLogManager") TabularLogManager logManager) {
		fYaoChurnId = yaoChurnId;
		fLog = logManager.get(YaoInit.class);
		fMode = mode(resolver, prefix, mode);
	}

	@Override
	public boolean execute() {
		// Distributions used to generate the averages.
		IDistribution upAverage = new ShiftedPareto(ALPHA, BETA_UPTIME,
				CommonState.r);
		IDistribution downAverage = new ShiftedPareto(ALPHA, BETA_DOWNTIME,
				CommonState.r);
		// Assigns different distributions to each node.
		for (int i = 0; i < Network.size(); i++) {
			Node current = Network.get(i);
			YaoOnOffChurn churn = (YaoOnOffChurn) current
					.getProtocol(fYaoChurnId);
			double li = upAverage.sample();
			double di = downAverage.sample();
			IDistribution uptime = fMode.uptimeDistribution(li);
			IDistribution downtime = fMode.downtimeDistribution(di);
			churn.init(uptime, downtime, current);
			printParameters(i, current, li, di, uptime.expectation(),
					downtime.expectation());
		}
		return false;
	}

	private void printParameters(int index, Node node, double li, double di,
			double eli, double edi) {
		fLog.set("index", Integer.toString(index));
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
	private IMode mode(IResolver resolver, String prefix, String modeId) {
		for (IMode mode : modes) {
			if (mode.id().toUpperCase().equals(modeId)) {
				return mode;
			}
		}

		// Not one of the pre-set modes, interpret as class name.
		return (IMode) ObjectCreator.createInstance(
				Configuration.getClass(prefix + ".mode"), prefix, resolver);
	}
}
