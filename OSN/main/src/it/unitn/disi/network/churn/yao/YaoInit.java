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
 * href="http://dx.doi.org/10.1109/ICNP.2006.320196">original paper</a>. <BR>
 * <BR>
 * The implementation actually allows more flexibile initialization, but all
 * preset modes from the original paper are coded here.
 */
@StructuredLog(key = "YaoInit", fields = { "id", "index", "li", "di", "eli",
		"edi", "ai" })
@AutoConfig
public class YaoInit implements Control {

	// ------------------------------------------------------------------------
	// Configuration machinery.
	// ------------------------------------------------------------------------

	interface IDistributionGenerator {
		IDistribution uptimeDistribution(double li);

		IDistribution downtimeDistribution(double di);

		String id();
	}

	interface IAverageGenerator {
		double nextLI();

		double nextDI();

		String id();
	}

	// ------------------------------------------------------------------------
	// Preset average generators.
	// ------------------------------------------------------------------------

	/**
	 * Alpha parameters for the shifted Pareto distributions used to generate
	 * uptime and downtime averages.
	 */
	private static final double ALPHA = 3.0;

	private static final double BETA_UPTIME = 1.0;

	private static final double BETA_DOWNTIME = 2.0;

	private static final IAverageGenerator YAO_GENERATOR = new AverageGeneratorImpl(
			new ShiftedPareto(ALPHA, BETA_UPTIME, CommonState.r),
			new ShiftedPareto(ALPHA, BETA_DOWNTIME, CommonState.r), "yao");

	private static final IAverageGenerator[] generators = new IAverageGenerator[] { YAO_GENERATOR };

	// ------------------------------------------------------------------------
	// Preset system modes.
	// ------------------------------------------------------------------------

	// -- Heavy tailed
	private static final IDistributionGenerator HEAVY_TAILED = new DualPareto(
			3.0, 3.0, 2.0, 2.0, "H", CommonState.r);

	// -- Very Heavy tailed
	private static final IDistributionGenerator VERY_HEAVY_TAILED = new DualPareto(
			1.5, 1.5, 2.0, 2.0, "VH", CommonState.r);

	// -- Exponential System
	private static final IDistributionGenerator EXPONENTIAL_SYSTEM = new IDistributionGenerator() {

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

	private static final IDistributionGenerator[] modes = new IDistributionGenerator[] {
			HEAVY_TAILED, VERY_HEAVY_TAILED, EXPONENTIAL_SYSTEM };

	// ------------------------------------------------------------------------

	private int fYaoChurnId;

	private TableWriter fLog;

	private final IDistributionGenerator fMode;

	private final IAverageGenerator fAverages;

	public YaoInit(
			@Attribute(Attribute.PREFIX) String prefix,
			@Attribute IResolver resolver,
			@Attribute("protocol") int yaoChurnId,
			@Attribute("mode") String mode,
			@Attribute(value = "generator", defaultValue = "yao") String generator,
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
			Node current = Network.get(i);
			YaoOnOffChurn churn = (YaoOnOffChurn) current
					.getProtocol(fYaoChurnId);
			double li = fAverages.nextLI();
			double di = fAverages.nextDI();
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
	private IDistributionGenerator mode(IResolver resolver, String prefix,
			String modeId) {
		for (IDistributionGenerator mode : modes) {
			if (mode.id().toUpperCase().equals(modeId)) {
				return mode;
			}
		}

		// Not one of the pre-set modes, interpret as class name.
		return (IDistributionGenerator) ObjectCreator.createInstance(
				Configuration.getClass(prefix + ".mode"), prefix, resolver);
	}

	@SuppressWarnings("unchecked")
	private IAverageGenerator generator(IResolver resolver, String prefix,
			String modeId) {
		for (IAverageGenerator generator : generators) {
			if (generator.id().toUpperCase().equals(modeId)) {
				return generator;
			}
		}

		// Not one of the pre-set modes, interpret as class name.
		return (IAverageGenerator) ObjectCreator
				.createInstance(Configuration.getClass(prefix + ".generator"),
						prefix, resolver);
	}
}
